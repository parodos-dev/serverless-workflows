# MTA - migration analysis workflow

# Synopsis
This workflow is an assessment workflow type, that invokes an application analysis workflow using [MTA][1]
and returns the [move2kube][3] workflow reference, to run next if the analysis is considered to be successful.

Users are encouraged to use this workflow as self-service alternative for interacting with the MTA UI. Instead of running
a mass-migration of project from a managed place, the project stakeholders can use this (or automation) to regularly check
the cloud-readiness compatibility of their code.

# Workflow application configuration
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `BACKSTAGE_NOTIFICATIONS_URL`      | The backstage server URL for notifications | ✅ | |
| `NOTIFICATIONS_BEARER_TOKEN`      | The authorization bearer token to use to send notifications | ✅ | |
| `MTA_URL`      | The MTA Hub server URL | ✅ | |


# Inputs
- `repositoryUrl` [mandatory] - the git repo url to examine
- `recipients` [mandatory] - A list of recipients for the notification in the format of `user:<namespace>/<username>` or `group:<namespace>/<groupname>`, i.e. `user:default/jsmith`.

# Output
1. On completion the workflow returns an [options structure][2] in the exit state of the workflow (also named variables in SonataFlow)
linking to the [move2kube][3] workflow that will generate k8s manifests for container deployment.
1. When the workflow completes there should be a report link on the exit state of the workflow (also named variables in SonataFlow)
Currently this is working with MTA version 6.2.x and in the future 7.x version the report link will be removed or will be made
optional. Instead of an html report the workflow will use a machine friendly json file.

# Dependencies
- MTA version 6.2.x or Konveyor 0.2.x

    - For OpenShift install MTA using the OperatorHub, search for MTA. Documentation is [here][1]
    - For Kubernetes install Konveyor with olm
      ```bash
      kubectl create -f https://operatorhub.io/install/konveyor-0.2/konveyor-operator.yaml
      ```
# Runtime configuration

| key                                                  | default                                                                                      | description                               |
|------------------------------------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------|
| mta.url                                              | http://mta-ui.openshift-mta.svc.cluster.local:8080                                           | Endpoint (with protocol and port) for MTA |
| quarkus.rest-client.mta_json.url                     | ${mta.url}/hub                                             | MTA hub api                               |
| quarkus.rest-client.notifications.url                | ${BACKSTAGE_NOTIFICATIONS_URL:http://backstage-backstage.rhdh-operator/api/notifications/} | Backstage notification url                |
| quarkus.rest-client.mta_json.auth.basicAuth.username | username                                                                                     | Username for the MTA api                  |
| quarkus.rest-client.mta_json.auth.basicAuth.password | password                                                                                     | Password for the MTA api                  |

All the configuration items are on [./application.properties]

For running and testing the workflow refer to [mta testing](https://github.com/parodos-dev/serverless-workflows/tree/main/mta-v7.x#output).

# Workflow Diagram
![mta workflow diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/mta-v7.x/mta.svg?raw=true)

# Installation

See [official installation guide](https://github.com/parodos-dev/serverless-workflows-config/blob/main/docs/main/mta-v7.x)

[1]: https://developers.redhat.com/products/mta/download
[2]: https://github.com/parodos-dev/serverless-workflows/blob/main/assessment/schema/workflow-options-output-schema.json  
[3]: https://github.com/parodos-dev/serverless-workflows/tree/main/move2kube
