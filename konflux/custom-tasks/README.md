This folder contains all custom tasks used in the konflux pipelines.

Tu build an image based on the task:
1. Install tekton ci: https://tekton.dev/docs/cli/
1. Create the repository in which the image should be pushed
1. Run `
kn bundle push  quay.io/orchestrator/replace-me:replace-me -f .tekton/tasks/replace-me.yaml
`
1. Update [the task policy data](../policy-data/task-bundles.yaml.yaml) in order for the task to be updated (see [Entreprise Contract doc about trusting custom task](https://enterprisecontract.dev/docs/ec-policies/trusting_tasks.html#_adding_a_custom_task_to_the_trusted_task_list))

See [how konflux-ci are building their task](https://github.com/konflux-ci/build-definitions/blob/main/hack/build-and-push.sh).