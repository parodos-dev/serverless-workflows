This folder contains all custom tasks used in the konflux pipelines.

Tu build an image based on the task:
1. Install tekton ci: https://tekton.dev/docs/cli/
1. Create the repository in which the image should be pushed
1. Run `
tkn bundle push  quay.io/orchestrator/konflux-task-copy-ta:vXXX -f konflux/custom-tasks/copy-task.yaml
`
1. Update [the task policy data](../policy-data/task-bundles.yaml.yaml) in order for the task to be updated (see [Entreprise Contract doc about trusting custom task](https://enterprisecontract.dev/docs/ec-policies/trusting_tasks.html#_adding_a_custom_task_to_the_trusted_task_list))
1. Once merged, create a MR to update https://gitlab.cee.redhat.com/releng/konflux-release-data/-/blob/main/config/stone-prd-rh01.pg1f.p1/product/EnterpriseContractPolicy/registry-orchestrator-releng-serverless-workflows.yaml#L28 with the new sha pointing to the version to use
See [how konflux-ci are building their task](https://github.com/konflux-ci/build-definitions/blob/main/hack/build-and-push.sh).