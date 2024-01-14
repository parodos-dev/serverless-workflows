# serverless-workflows

A selected set of serverless workflows

The strucure of this repo a directory per workflow at the root. Each folder
contains at least:
- `application.properties` the configution item specific for the workflow app itself.
- `${workflow}.sw.yaml`    the serverless workflow definitions[1]. 
- `specs/`                 optional folder with openapi specs if the flow needs them. 

All .svg can be ignored, there's no real functional use for them in deployment
and all of them are created by vscode extension. TODO remove all svg and gitignore them.

Every workflow has a matching container image pushed to quay.io by a github workflows
in the form of `quay.io/orchestrator/serverless-workflow-${workflow}`.

## Current image statuses:

- [![Docker Repository on Quay](https://quay.io/repository/orchestrator/serverless-workflow-mta/status "Docker Repository on Quay")](https://quay.io/repository/orchestrator/serverless-workflow-mta)
- [![Docker Repository on Quay](https://quay.io/repository/orchestrator/serverless-workflow-m2k/status "Docker Repository on Quay")](https://quay.io/repository/orchestrator/serverless-workflow-m2k) 
- [![Docker Repository on Quay](https://quay.io/repository/orchestrator/serverless-workflow-greeting/status "Docker Repository on Quay")](https://quay.io/repository/orchestrator/serverless-workflow-greeting) 
After image publishing, github action will generate kubernetes manifests and push a PR to github.com/rgolangh/sreverless-workflows-helm
under a directory matching the workflow name. This repo is used to deploy the workflows to an environment 
with Sonataflow operator[2] running. 

## To introduce a new workflow
1. create a folder under the root with the name of the flow, e.x `/onboarding`
2. copy `application.properties`, `onboarding.sw.yaml` into that folder  
3. create a pull request but don't merge yet.
4. Send a pull request to https://github.com/rgolangh/serverless-workflows-helm to add a subchart 
   under the path `charts/workflows/charts/onboarding`. You can copy the greeting subchart directory and files. 
5. Create a PR to serverless-workflows-helm and make sure its merge.
6. Now the PR from 3 can be merged and an automatic PR will be created with the generated manifests. Review and merge. 

   
[1]: https://github.com/serverlessworkflow/specification/blob/main/specification.md
[2]: https://github.com/apache/incubator-kie-kogito-serverless-operator/

