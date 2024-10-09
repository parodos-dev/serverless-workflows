# Move2kube (m2k) workflow
## Context
This workflow is using https://move2kube.konveyor.io/ to migrate the existing code contained in a git repository to a K8s/OCP platform.

Once the transformation is over, move2kube provides a zip file containing the transformed repo.

### Design diagram
![sequence_diagram.svg](https://raw.githubusercontent.com/parodos-dev/serverless-workflows/main/move2kube/sequence_diagram.jpg)
![design.svg](https://raw.githubusercontent.com/parodos-dev/serverless-workflows/main/move2kube/design.svg)

### Workflow
![m2k.svg](https://raw.githubusercontent.com/parodos-dev/serverless-workflows/main/move2kube/m2k.svg)

Note that if an error occurs during the migration planning there is no feedback given by the move2kube instance API. To overcome this, we defined a maximum amount of retries  (`move2kube_get_plan_max_retries`) to execute while getting the planning before exiting with an error. By default the value is set to 10 and it can be overridden with the environment variable `MOVE2KUBE_GET_PLAN_MAX_RETRIES`.

## Workflow application configuration
### Move2kube workflow
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `MOVE2KUBE_URL`       | The  move2kube instance server URL | ✅ | |
| `BACKSTAGE_NOTIFICATIONS_URL`      | The backstage server URL for notifications | ✅ | |
| `NOTIFICATIONS_BEARER_TOKEN`      | The authorization bearer token to use to send notifications | ✅ | |
| `MOVE2KUBE_GET_PLAN_MAX_RETRIES`      | The amount of retries to get the plan before failing the workflow | ❌ | 10 |


### m2k-func serverless function
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `MOVE2KUBE_API`       | The move2kube instance server URL | ✅ | |
| `SSH_PRIV_KEY_PATH`      | The absolute path to the SSH private key | ✅ | |
| `BROKER_URL`      | The knative broker URL | ✅ | |
| `LOG_LEVEL`      | The log level | ❌ | INFO |


## Components
The use case has the following components:
1. `m2k`: the `Sonataflow` resource representing the workflow. A matching `Deployment` is created by the sonataflow operator..
2. `m2k-save-transformation-func`: the Knative `Service` resource that holds the service retrieving the move2kube instance output and saving it to the git repository. A matching `Deployment` is created by the Knative deployment.
3. `move2kube instance`: the `Deployment` running the move2kube instance
4. Knative `Trigger`:
   1. `m2k-save-transformation-event`: event sent by the `m2k` workflow that will trigger the execution of `m2k-save-transformation-func`.
   2. `transformation-saved-trigger-m2k`: event sent by `m2k-save-transformation-func` if/once the move2kube output is successfully saved to the git repository.
   3. `error-trigger-m2k`: event sent by `m2k-save-transformation-func` if an error while saving the move2kube output to the git repository.
5. The Knative `Broker` named `default` which link the components together.

## Installation

See [official installation guide](https://github.com/parodos-dev/serverless-workflows-config/blob/main/docs/main/move2kube)

## Usage
1. Create a workspace and a project under it in your move2kube instance
   * you can reach your move2kube instance by running
   ```bash
   oc -n sonataflow-infra get routes
   ```
   Sample output:
    ```
    NAME                                   HOST/PORT                                                                                             PATH   SERVICES                                 PORT    TERMINATION   WILDCARD
    move2kube-route                        move2kube-route-sonataflow-infra.apps.cluster-c68jb.dynamic.redhatworkshops.io                               move2kube-svc                            <all>   edge          None
    ```
   * for more information, please refer to https://move2kube.konveyor.io/tutorials/ui
2. Go to the backstage instance.

To get it, you can run 
```bash
oc -n rhdh-operator get routes
```
Sample output:
```
NAME                  HOST/PORT                                                                            PATH   SERVICES              PORT           TERMINATION     WILDCARD
backstage-backstage   backstage-backstage-rhdh-operator.apps.cluster-c68jb.dynamic.redhatworkshops.io   /      backstage-backstage   http-backend   edge/Redirect   None
```
3. Go to the `Orchestrator` page. 

4. Click on `Move2Kube workflow` and then click the `run` button on the top right of the page.
5. In the `repositoryURL` field, put the URL of your git project
   * ie: https://bitbucket.org/parodos/m2k-test
6. In the `sourceBranch` field, put the name of the branch holding the project you want to transform
   * ie: `main`
7. In the `targetBranch` field, put the name of the branch in which you want the move2kube output to be persisted. If the branch exists, the workflow will fail
   * ie: `move2kube-output`
8. In the `workspaceId` field, put the ID of the move2kube instance workspace to use for the transformation. Use the ID of the workspace created at the 1st step.
   * ie: `a46b802d-511c-4097-a5cb-76c892b48d71`
9. In the `projectId` field, put the ID of the move2kube instance project under the previous workspace to use for the transformation. Use the ID of the project created at the 1st step.
   * ie: `9c7f8914-0b63-4985-8696-d46c17ba4ebe`
10. Then click on `nextStep` 
11. Click on `run` to trigger the execution
12. Once a new transformation has started and is waiting for your input, you will receive a notification with a link to the Q&A
      * For more information about what to expect and how to answer the Q&A, please visit [the official move2kube documentation](https://move2kube.konveyor.io/tutorials/ui)
13. Once you completed the Q&A, the process will continue and the output of the transformation will be saved in your git repository, you will receive a notification to inform you of the completion of the workflow.
      * You can now clone the repository and checkout the output branch to deploy your manifests to your cluster! You can check [the move2kube documention](https://move2kube.konveyor.io/tutorials/cli#deploying-the-application-to-kubernetes-with-the-generated-artifacts) if you need guidance on how to deploy the generated artifacts.
