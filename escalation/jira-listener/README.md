# Jira listener
An application to monitor Jira webhooks and send a CloudEvent to the configured event sink whenever they match these requirements:
* They refer to closed tickets (e.g. status category key is `done`)
* They contain the labels added by the `Escalation workflow` application, e.g.:
  * `workflowInstanceId=<SWF instance ID>`
  * `workflowName=escalation`

The generated event includes only the relevant data, e.g.:
```json
{
    "ticketId":"ES-3",
    "workFlowInstanceId":"500",
    "workflowName":"escalation",
    "status":"done"
}  
```

No events are generated for discarded webhooks.

## Design notes

### Externalized configuration
The following environment variables can modify the configuration properties:

| Variable | Description | Default value |
|----------|-------------|---------------|
| CLOUD_EVENT_TYPE | The value of `ce-type` header in the generated `CloudEvent` | `dev.parodos.escalation` |
| CLOUD_EVENT_SOURCE | The value of `ce-source` header in the generated `CloudEvent` | `jira.listener` |
| WORKFLOW_INSTANCE_ID_LABEL | The name part of the Jira ticket label that contains the ID of the relates SWF instance (e.g. `workflowInstanceId=123`)  | `workflowInstanceId` |
| WORKFLOW_NAME_LABEL | The name part of the Jira ticket label that contains the name of the SWF (e.g. `workflowName=escalation`)  | `workflowName` |
| EXPECTED_WORKFLOW_NAME | The expected value part of the Jira ticket label that contains the name of the SWF (e.g. `workflowName=escalation`)  | `escalation` |
| K_SINK | The URL where to POST the generated `CloudEvent` (usually injected by the `SinkBinding` resource) | - |

### Event modeling
Instead of leveraging on the [Jira Java SDK](https://developer.atlassian.com/server/jira/platform/java-apis/), we used a simplified model of the relevant data,
defined in the [WebhookEvent](./src/main/java/dev/parodos/jiralistener/model/WebhookEvent.java) Java class. This way we can simplify the dependency stack
and also limit the risk of parsing failures due to unexpected changes in the payload format.

Parsing was derived from the original example in [this Backstage repo](https://github.com/tiagodolphine/backstage/blob/eedfe494dd313a3ad6a484c0596ba12d6199c1a8/plugins/swf-backend/src/service/JiraService.ts#L66C19-L66C40)

## Building and publishing the image
The application runs from a containerized image already avaliable at `quay.io/orchestrator/jira-listener-jvm`.
You can build and publish your own image using:
```bash
mvn clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/jira-listener-jvm .
docker tag quarkus/jira-listener-jvm quay.io/_YOUR_QUAY_ID_/jira-listener-jvm
docker push quay.io/_YOUR_QUAY_ID_/jira-listener-jvm
```

## Running in development environment
Use this command to run the example at `localhost:8080` with remote debugger enabled at `5005`:
```bash
mvn quarkus:dev
```

## Deploying as Knative service
Follow the instructions at [Deploying the example](../README.md#deploying-the-example)

### SSL
If you enabled the automatic route creation in Knative services, you can probably hit this error if you try to `curl` to its `https` endpoint and
OpenShift is publishing a self signed certificate:
```
curl failed to verify the legitimacy of the server and therefore could not
establish a secure connection to it. To learn more about this situation and
how to fix it, please visit the web page mentioned above.
```

Since "Jira Cloud's built-in webhooks can handle sending requests over SSL to hosts using publicly signed certificates", we need to disable the automatic Route
creation using the following annotation in the `jira-listener` service:
```yaml
  annotations:
    serving.knative.openshift.io/disableRoute: "true"
```
Then, we use the [Let's Encrypt](https://letsencrypt.org/) service to leverage its free publicly-signed certificates, according to this
[Securing Jira Webhooks discussion](https://community.atlassian.com/t5/Jira-questions/Securing-Jira-Webhooks/qaq-p/1850259)

The following procedure is not integrated with the provided Helm charts and comes from this [article](https://developer.ibm.com/tutorials/secure-red-hat-openshift-routes-with-lets-encrypt/):
```bash
oc new-project acme-operator
oc create -n acme-operator \
  -fhttps://raw.githubusercontent.com/tnozicka/openshift-acme/master/deploy/cluster-wide/{clusterrole,serviceaccount,issuer-letsencrypt-live,deployment}.yaml
oc create clusterrolebinding openshift-acme --clusterrole=openshift-acme --serviceaccount="$( oc project -q ):openshift-acme" --dry-run -o yaml | oc create -f -
```

The provided Helm chart instead generates a Route in `knative-serving-ingress` namespace with the proper annotation to expose the publicly-signed certificate, e.g.:
```yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  annotations:
    haproxy.router.openshift.io/timeout: 600s 
    kubernetes.io/tls-acme: "true"
  name: jira-listener 
  namespace: knative-serving-ingress 
...
```

Run the following to uninstall the Let's Encrypt operator:
```bash
oc delete clusterrolebinding openshift-acme
oc delete -n acme-operator \
  -fhttps://raw.githubusercontent.com/tnozicka/openshift-acme/master/deploy/cluster-wide/{clusterrole,serviceaccount,issuer-letsencrypt-live,deployment}.yaml
oc delete project acme-operator
```

## Testing with curl
Initialize the `JIRA_WEBHOOK_URL` variable in case of local development environment:
```bash
JIRA_WEBHOOK_URL="http://localhost:8080/webhook/jira"
```
Otherwise, in case of Knative environment:
```bash
JIRA_LISTENER_URL=$(oc get route -n knative-serving-ingress jira-listener -oyaml | yq '.status.ingress[0].host')
JIRA_WEBHOOK_URL="https://${JIRA_LISTENER_URL//\"/}/webhook/jira"
```

Then, use one of the sample json documents in [src/test/resources](./src/test/resources/) to trigger the `/webhook/jira` endpoint:
```bash
curl -v -X POST -d @./src/test/resources/valid.json -H "Content-Type: application/json" -k  "${JIRA_WEBHOOK_URL}"
curl -v -X POST -d @./src/test/resources/invalid.json -H "Content-Type: application/json" -k  "${JIRA_WEBHOOK_URL}"
```

### Troubleshooting the Duplicate Certificate Limit error
`Let's Encrypt` allows 5 certificate requests per week for each unique set of hostnames requested for the certificate.

The issue is detected when the `jira-listener` service is not receiving any webhook event, and the above `JIRA_WEBHOOK_URL` uses an `http`
protocol instead of the expected `https`.

To overcome this issue, you can define a different name for the `jira-listener` service by setting the property `jiralistener.name` as in:
```bash
helm upgrade -n default escalation-eda helm/escalation-eda --set jiralistener.name=my-jira-listener --debug 
```

### Troubleshooting the SAN short enough to fit in CN issue
Note that the created hostname cannot exceed the 64 characters as described in: [Let's Encrypt (NewOrder request did not include a SAN short enough to fit in CN)](https://support.cpanel.net/hc/en-us/articles/4405807056023-Let-s-Encrypt-NewOrder-request-did-not-include-a-SAN-short-enough-to-fit-in-CN-)
>This error occurs when attempting to request an SSL certificate from Let's Encrypt for a domain name longer than 64 characters

## Configuring the Jira server
### API token
In case you need to interact with Jira server using the [REST APIs])https://developer.atlassian.com/server/jira/platform/rest-apis/, you need an API Token:
* [API Tokens](https://id.atlassian.com/manage-profile/security/api-tokens)
* [Basic auth for REST APIs](https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/)

### Webhook
If you use Jira Cloud, you can create the webhook at https://_YOUR_JIRA_/plugins/servlet/webhooks, then:
* Configure `Issue related event` of type `update`
* Use the value of `JIRA_WEBHOOK_URL` calculated before as the URL

![Jira webhook](../doc/webhook.png)

The webhook event format is exaplained in [Issue: Get issue](https://docs.atlassian.com/software/jira/docs/api/REST/9.11.0/#api/2/issue-getIssue),
see an [Example](https://jira.atlassian.com/rest/api/2/issue/JRA-2000)

In case of issues receiving the events, you can troubleshoot using [RequestBin](https://requestbin.com/), see [How to collect data to troubleshoot WebHook failure in Jira](https://confluence.atlassian.com/jirakb/how-to-collect-data-to-troubleshoot-webhook-failure-in-jira-397083035.html)