# Serverless-Workflows

This repository contains multiple workflows. Each workflow is represented by a directory in the project. Below is a table listing all available workflows:

| Workflow Name                    | Description                                       |
|----------------------------------|---------------------------------------------------|
| `create-ocp-project`             | Sets up an OpenShift Container Platform (OCP) project. |
| `escalation`                     | Demos workflow ticket escalation.          |
| `extendable-workflow`            | Provides a flexible, extendable workflow setup.   |
| `greeting`                       | Sample greeting workflow.                         |
| `modify-vm-resources`            | Modifies resources allocated to virtual machines. |
| `move2kube`                      | Workflow for Move2Kube tasks and transformation.  |
| `mta-v7.x`                       | Migration toolkit for applications, version 7.x.  |
| `mtv-migration`                  | Migration tasks using Migration Toolkit for Virtualization (MTV). |
| `mtv-plan`                       | Planning workflows for Migration Toolkit for Virtualization. |
| `request-vm-cnv`                 | Requests and provisions VMs using Container Native Virtualization (CNV). |

Here is the layout of directories per workflow. Each folder contains at least:

- `application.properties` the configuration item specific for the workflow app itself.
- `${workflow}.sw.yaml` the [serverless workflow definitions][1] with respect to the [best practices][4].
- `specs/` optional folder with OpenAPI specs if the flow needs them.

All .svg can be ignored, there's no real functional use for them in deployment
and all of them are created by VSCode extension.

Every workflow has a matching container image pushed to quay.io by a github workflows
in the form of `quay.io/orchestrator/serverless-workflow-${workflow}`.

## Current image statuses:

- https://quay.io/repository/orchestrator/serverless-workflow-mta-v7.x
- https://quay.io/repository/orchestrator/serverless-workflow-m2k
- https://quay.io/repository/orchestrator/serverless-workflow-greeting
- https://quay.io/repository/orchestrator/serverless-workflow-escalation

After image publishing, GitHub action will generate kubernetes manifests and push a PR to [the workflows helm chart repo][3]
under a directory matching the workflow name. This repo is used to deploy the workflows to an environment
with [Sonataflow operator][2] running.

## How to introduce a new workflow

Follow these steps to successfully add a new workflow:

1. Create a folder under the root with the name of the flow, e.x `/onboarding`
2. Copy `application.properties`, `onboarding.sw.yaml` into that folder
3. Create a GitHub workflow file `.github/workflows/${workflow}.yaml` that will call `main` workflow (see greeting.yaml)
4. Create a pull request but don't merge yet.
5. Send a pull request to [serverless-workflows-config repository][3] to add a sub-chart
   under the path `charts/workflows/charts/onboarding`. You can copy the greeting sub-chart directory and files.
6. Create a PR to [serverless-workflows-config repository][3] and make sure its merge.
7. Now the PR from 4 can be merged and an automatic PR will be created with the generated manifests. Review and merge.

See [Continuous Integration with make](https://github.com/parodos-dev/serverless-workflows/blob/main/make.md) for implementation details of the CI pipeline.

### Builder image

There are two builder images under ./pipeline folder:

- workflow-builder-dev.Dockerfile - references nightly build image from `docker.io/apache/incubator-kie-sonataflow-builder:main` that doesn't required any authorization
- workflow-builder.Dockerfile - references OpenShift Serverless Logic builder image from registry.redhat.io which requires authorization.
  - To use this dockerfile locally, you must be logged to registry.redhat.io. To get access to that registry, follow:
    1. Get tokens [here](https://access.redhat.com/terms-based-registry/accounts). Once logged in to podman, you should be able to pull the image.
    2. Verify pulling the image [here](https://catalog.redhat.com/software/containers/openshift-serverless-1-tech-preview/logic-swf-builder-rhel8/6483079349c48023fc262858?architecture=amd64&image=65e1a56104e00058ecdd52eb&container-tabs=gti)

Note on CI:
For every PR merged in the workflow directory, a GitHub Action runs an image build to generate manifests, and a new PR is automatically generated in the [serverless-workflows-config repository][3]. The credentials used by the build process are defined as organization level secret, and the content is from a token on the helm repo with an expiry period of 60 days. Currently only the repo owner (rgolangh) can recreate the token. This should be revised.

[1]: https://github.com/serverlessworkflow/specification/tree/main?tab=readme-ov-file#documentation
[2]: https://github.com/apache/incubator-kie-kogito-serverless-operator/
[3]: https://github.com/parodos-dev/serverless-workflows-config
[4]: https://github.com/parodos-dev/serverless-workflows/blob/main/best-practices.md
