This folder contains all custom tasks used in the konflux pipelines.

Tu build an image based on the task:
1. Install tekton ci: https://tekton.dev/docs/cli/
1. Create the repository in which the image should be pushed
1. Run
```console
kn bundle push  quay.io/orchestrator/replace-me:replace-me -f .tekton/tasks/replace-me.yaml
```