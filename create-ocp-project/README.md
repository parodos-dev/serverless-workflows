# Create OpenShift Project workflow
The Create OpenShift Project workflow is a workflow that demonstrates the following features of the serverless workflow technology:
* Integration with external service, in this case, Jira Cloud via its OpenAPI
* Polling
* Conditional branching
* Using the Notifications plugin to send notifications to the user

The workflow creates a Jira issue and waits for its approval within 60s.
After creating the Jira issue, the workflow sends a notification to the default user to be aware of the issue.
The workflow also creates another Jira issue for auditing purposes, to be closed after the workflow is done.

If the Jira issues for approval is resolved within 60 seconds, the workflow continues to the operations.
If the Jira issue isn't resolved within 60 seconds, the workflow fires a timeout event.

This workflow can be extended to introduce more capabilities, such as creating K8s resource in OpenShift cluster.

## Input
- `Audit Jira Project Key` [required] - the Jira Project Key to which the workflow is configured to work and has permission to create and update and issue of type Task.
- `Operations Jira Project Key` [required] - the Jira Project Key to which the workflow is configured to work and has permission to create and update and issue of type Task.
- `OCP project to create` [required] - the OCP project to be created on the OCP cluster.

## Workflow diagram
![Create OpenShift Project diagram](https://github.com/parodos-dev/serverless-workflow-examples/blob/main/create-ocp-project/create-ocp-project.svg?raw=true)
