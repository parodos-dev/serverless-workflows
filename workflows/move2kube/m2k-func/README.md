# m2k-kfunc Project
This projects implements the Knative functions that will interact with Move2Kube instance and Github in order to prepare and save the transformations.

* SaveTransformationOutput:
  * Triggered by the event `save-transformation`
  * This function will first retrieve the transformation output archive from the Move2Kube project
  * Then it will create a new branch based on the provided input in the provided BitBucket repo
  * Finally, it will un-archive the previously downloaded file, commit the change and push them to BitBucket using the ssh keys provided.
  * Will send events:
    * `transformation_saved` if success
    * `error` if any error
## Integration tests
Those function will be tested by the integration tests of the whole project. But you can run manual ones!

To run the integration test you need to be sure to have
1. A running move2kube instance:

```bash
docker run --rm -it -p 8080:8080 quay.io/konveyor/move2kube-ui
```
2. To have access to BitBucket


## Install

Run the following command to execute tests and install the project:
```bash
mvn clean install
```
## Build image
To build the image, run:
```bash
 docker build -t quay.io/orchestrator/m2k-kfunc:2.0.0-SNAPSHOT -f src/main/docker/Dockerfile.jvm .
```

## Run it
### Prerequisites
Make sure you have a local K8s cluster with Knative installed on it, see https://knative.dev/docs/install/quickstart-install/#run-the-knative-quickstart-plugin

Make sure you have a running instance of move2kube reachable from the cluster:
```bash
kubectl run fedora --rm --image=fedora -i --tty -- bash

curl -XGET <your move2kube instance>
```

### Deploy Knative functions to  cluster

* [m2k-service.yaml](k8s/m2k-service.yaml) will deploy 2 kservices that will spin-up the functions when an event is received
* [m2k-trigger.yaml](k8s/m2k-trigger.yaml) will deploy the triggers in order to susbcribe to the events used by the Knative services


First create the roles:
```bash
kubectl apply -f k8s/m2k-role.yaml 
```
Should output
```
role.rbac.authorization.k8s.io/service-discovery-role created
rolebinding.rbac.authorization.k8s.io/serverless-workflow-m2k-service-discovery-role created
rolebinding.rbac.authorization.k8s.io/service-discovery-rolebinding created
```
Then the Knative services:
```bash
kubectl -n m2k apply -f k8s/m2k-service.yaml 
```
Should output
```
service.serving.knative.dev/m2k-save-transformation-func created
```
Finally the triggers
```bash
kubectl -n m2k apply -f k8s/m2k-trigger.yaml 
```
Should output
```
trigger.eventing.knative.dev/m2k-save-transformation-event created
```
You will notice that the environment variable `EXPORTED_FUNC` is set for each Knative service: this variable defines which function is expose in the service.

To run properly, a move2kube instance must be running in the cluster, or at least reachable from the cluster:
```bash
kubectl apply -f k8s/move2kube.yaml
```
Should output
```
deployment.apps/move2kube created
service/move2kube-svc created
```

You can access it locally with port-forward:
```bash
kubectl port-forward  svc/move2kube-svc 8080:8080 &
```

By default, the Knative function will use `http://move2kube-svc.default.svc.cluster.local:8080/api/v1` as host to reach the move2kube instance.
You can override this value by setting environment variable `MOVE2KUBE_API`

You should have something similar to:
```bash
kubectl -n m2k get ksvc
```
Should output
```
NAME                           URL                                                               LATESTCREATED                     LATESTREADY                       READY   REASON
m2k-save-transformation-func   http://m2k-save-transformation-func.m2k.10.110.165.153.sslip.io   m2k-save-transformation-func-v1   m2k-save-transformation-func-v1   True    
```
### Use it
You shall sent the following requests from within the K8s cluster, to do so, you could run:
```bash
kubectl run fedora --rm --image=fedora -i --tty -- bash
```

1. Go to `http://<move2kubeUI-URL>/` and create a new workspace and a new project inside this workspace.

2. Create a plan by upload an archive (ie: zip file) containing a git repo (see https://move2kube.konveyor.io/tutorials/ui for more details)
3. Then start the transformation. 
You should be asked to answer some questions, once this is done, the transformation output should be generated.

4. To save a transformation output, send the following request from a place that can reach the broker deployed in the cluster:
```bash
curl -v "http://broker-ingress.knative-eventing.svc.cluster.local/m2k/default"\
 -X POST\
    -H "Ce-Id: 1234"\
    -H "Ce-Specversion: 1.0"\
    -H "Ce-Type: save-transformation"\
    -H "Ce-Source: curl"\
    -H "Content-Type: application/json"\
    -d '{"gitRepo": "<repo>", 
    "branch": "<branch to which save the transformation output>",
    "token": "<optional, bitbucket token with read/write rights, otherwise will use ssh key>",
    "workspaceId": "<ID of the workspace previously created>",
    "projectId": "<ID of the project previously created>",
    "transformId": "<ID of the transformation previously created>",
    "workflowCallerId": "<string, represents the ID of the SWF calling>"
    }'
```
You should see a new pod created for the save transformation service:

```bash
kubectl get pods -n m2k 
```
Should output
```
NAME                                                         READY   STATUS    RESTARTS   AGE
m2k-save-transformation-func-v1-deployment-76859dc76-h7856   2/2     Running   0          6s
```

After few minutes, the pods will automatically scale down if no new event is received.

The URL `http://broker-ingress.knative-eventing.svc.cluster.local/m2k/default` is formatted as follow: `http://broker-ingress.knative-eventing.svc.cluster.local/<namespace>/<broker name>`. If you were to change the namespace or the name of the broker, the URL should be updated accordingly.

To get this URL, run
```bash
kubectl get broker -n m2k 
```
Should output
```
NAME      URL                                                                    AGE    READY   REASON
default   http://broker-ingress.knative-eventing.svc.cluster.local/m2k/default   107s   True    
```

## Move2Kube API version

Note that the client used in this function to call move2kube-api is generated from the openapi file under `src/main/resources/move2kube-openapi.yaml`
To update or use a different version see https://raw.githubusercontent.com/konveyor/move2kube-api/main/assets/openapi.json

## Related Guides

- Funqy HTTP Binding ([guide](https://quarkus.io/guides/funqy-http)): HTTP Binding for Quarkus Funqy framework

