
# Continuous Integration with make
Continuous integration of the workflows repository automates the jobs to deploy the workflows in the target cluster:
* Build the containerized image of the workflow and the related applications
* Push the containerized image to an image registry
* Generate the K8s manifests for the workflow
* Customize the manifests according to the selected deployment option (either `kustomize` or `helm`)
* Commit the manifests and the deployment changes to a configurable deployment repository

Generating the PR for the pushed commit is out of the scope of the make procedure, as it depends
on the specific git service provider (e.g. GitHub rather than Bitbucket).

## Prerequisites
* `make` version 3.8x
* `docker` or `podman` containers
* Linux OS
* Container registry credentials to publish the generated image

The actuall implementation is delegated to shell scripts localted under the [scripts](./scripts/) folder that run
in a containerized image including all the required dependencies: there's no need to install any other 
tool in the local environment. 

## Configuration variables
Variables can be used to configure the behavior of the [Makefile](./Makefile):

| Variable | Description | Default |
|----------|-------------|---------|
| WORKFLOW_ID | The workflow ID | **Must be provided** |
| APPLICATION_ID | The application ID | undefined |
| CONTAINER_ENGINE | The container engine | `podman` if available, otherwise `docker` |
| WORKDIR | The working directory where the repo files are copied | Execution of `mktemp -d` |
| GIT_USER_NAME | Git username | Username from `git config` |
| GIT_USER_EMAIL | Git user email | User email from `git config` |
| PR_OR_COMMIT_URL | URL to use in commit message | `<remote URL>/commits/<commit ID>` from local repo |
| GIT_TOKEN | Git credentials token to push the commit | Content of `.git_token` file |
| LINUX_IMAGE | Linux UBI image to run the pipeline | `quay.io/orchestrator/ubi9-pipeline:latest` |
| JDK_IMAGE | JDK image to build the Java applications | `registry.access.redhat.com/ubi9/openjdk-17:1.17` |
| BUILD_APPLICATION_SCRIPT | The application-specific build script | `scripts/build_application.sh` |
| DOCKERFILE | Relative path to the Dockerfile file.<br/>From the repository root for workflows.<br/>From the application root for applications  | `pipeline/workflow-builder.Dockerfile` for workflows.</br>`src/main/docker/Dockerfile.jvm` for applications |
| MVN_OPTS | Maven build options for Java applications | `-B` |
| REGISTRY | Container registry to publish the image | `quay.io` |
| REGISTRY_REPO | Container registry repository name | Name of current OS user (e.g. `whoami`) |
| REGISTRY_USERNAME | Container registry username | `""` (e.g., no login attempted) |
| REGISTRY_PASSWORD | Container registry user password | `""`  (e.g., no login attempted) |
| IMAGE_PREFIX | Automatically added image prefix | `serverless-workflow` |
| IMAGE_TAG | Automatically added image tag | 8 chars commit hash of the latest commit |
| DEPLOYMENT_REPO | Git repo of the deployment source code | `parodos-dev/serverless-workflows-helm` |
| DEPLOYMENT_BRANCH | Branch of the deployment git repo | `main` |

Override the default values with:
```bash
make <VAR1>=<VALUE1> ... <VARn>=<VALUEn> <TARGET> 
```

### Requirements for WORKFLOW_ID variable
* Must match one of the workflows folder names, like [escalation](./escalation)
* Must contain a valid, workflow in a flat layout (e.g., no Java code, not a Maven-Quarkus project)

### Requirements for APPLICATION_ID variable
* Must match one of the application folders under the given workflow folder, like [jira-listener](./escalation/jira-listener/)
* Must contain a valid, Maven Java project
* Must be compatible with the selected `JDK_IMAGE`
 
### Requirements for Linux UBI image
See the [setup](./setup/README.md) documentation.

### Requirements for the deployment repo
The procedure assumes that the folder structure of the target deployment repository reflects the one of the [default repository](https://github.com/parodos-dev/serverless-workflows-helm), e.g.:
* `kustomize` projects are located under the `kustomize/WORKFLOW_ID` folder
  * Manifests are stored in the `base` subfolder
  * Image is customized in the `overlays/prod` subfolder
* `helm` projects are located under the `charts/workflows/charts/WORKFLOW_ID` folder
  * Manifests are copied under the `templates` subfolder with no Helm-specific manipulation

## Building with make
The following examples show how to build a specific workflow like `escalation` in the local repository.

### Building a Workflow
Build the image, push it to the registry, prepare the manifests and update the kustomize project with a commit pushed to the default deployment repository:
```bash
make WORKFLOW_ID=escalation
```

Same as above but using `docker`:
```bash
make CONTAINER_ENGINE=docker WORKFLOW_ID=escalation
```

Build the workflow image:
```bash
make CONTAINER_ENGINE=docker WORKFLOW_ID=escalation build-image
```

Build and push the workflow image:
```bash
make CONTAINER_ENGINE=docker WORKFLOW_ID=escalation build-image push-image
```

Push the workflow image (assuming that the above command was executed before):
```bash
make CONTAINER_ENGINE=docker WORKFLOW_ID=escalation push-image
```

Generate the k8s manifests in the WORKDIR folder:
```bash
make CONTAINER_ENGINE=docker WORKFLOW_ID=escalation gen-manifests
```
Generate the k8s manifests and push them to the default deployment repo:
```bash
make CONTAINER_ENGINE=docker WORKFLOW_ID=escalation gen-manifests push-manifests
```

### Building an Application
Use the same commands as above and include the `APPLICATION_ID` variable, as in:
```bash
make WORKFLOW_ID=escalation APPLICATION_ID=jira-listener
```





