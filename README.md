# serverless-workflows

A selected set of serverless workflows

The structure of this repo a directory per workflow at the root. Each folder
contains at least:
- `application.properties` the configuration item specific for the workflow app itself.
- `${workflow}.sw.yaml`    the [serverless workflow definitions][1].
- `specs/`                 optional folder with OpenAPI specs if the flow needs them.

All .svg can be ignored, there's no real functional use for them in deployment
and all of them are created by VSCode extension. TODO remove all svg and gitignore them.

Every workflow has a matching container image pushed to quay.io by a github workflows
in the form of `quay.io/orchestrator/serverless-workflow-${workflow}`.

## Current image statuses:

- https://quay.io/repository/orchestrator/serverless-workflow-mta
- https://quay.io/repository/orchestrator/serverless-workflow-m2k 
- https://quay.io/repository/orchestrator/serverless-workflow-greeting
- https://quay.io/repository/orchestrator/serverless-workflow-escalation


After image publishing, github action will generate kubernetes manifests and push a PR to [the workflows helm chart repo][3]
under a directory matching the workflow name. This repo is used to deploy the workflows to an environment 
with [Sonataflow operator][2] running. 

## To introduce a new workflow
1. create a folder under the root with the name of the flow, e.x `/onboarding`
2. copy `application.properties`, `onboarding.sw.yaml` into that folder  
3. create a github workflow file `.github/workflows/${workflow}.yaml` that will call `main` workflow (see greeting.yaml) 
4. create a pull request but don't merge yet.
5. Send a pull request to [the helm chart repo][3] to add a sub-chart 
   under the path `charts/workflows/charts/onboarding`. You can copy the greeting sub-chart directory and files. 
6. Create a PR to [serverless-workflows-config][3] and make sure its merge.
7. Now the PR from 4 can be merged and an automatic PR will be created with the generated manifests. Review and merge. 
   
See [Continuous Integration with make](make.md) for implementation details of the CI pipeline.

### Builder image
There are two builder images under ./pipeline folder:
- workflow-builder-dev.Dockerfile - references nightly build image from quay.io/kiegroup/kogito-swf-builder-nightly that doesn't required any authorization
- workflow-builder.Dockerfile - references OpenShift Serverless Logic builder image from registry.redhat.io which requires authorization.
  - To use this dockerfile locally, you must be logged to registry.redhat.io. To get access to that registry, follow:
    1. Get tokens [here](https://access.redhat.com/terms-based-registry/accounts). Once logged in to podman, you should be able to pull the image.
    2. Verify pulling the image [here](https://catalog.redhat.com/software/containers/openshift-serverless-1-tech-preview/logic-swf-builder-rhel8/6483079349c48023fc262858?architecture=amd64&image=65e1a56104e00058ecdd52eb&container-tabs=gti)


Note on CI:
On each merge under a workflow directory a matching github workflow executes 
an image build, generating manifests and a PR create on the [helm chart repo][3]. 
The credentials of this repo are an org level secret, and the content is from a token 
on the helm repo with an expiry period of 60 days. Currently only the repo owner (rgolangh) can 
recreate the token. This should be revised. 

[1]: https://github.com/serverlessworkflow/specification/blob/main/specification.md
[2]: https://github.com/apache/incubator-kie-kogito-serverless-operator/
[3]: https://github.com/parodos-dev/serverless-workflows-config
