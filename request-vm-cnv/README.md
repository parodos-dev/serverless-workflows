# This workflow is under development and not (yet) fit for PROD purposes, only for DEMO
# Request VM on CNV workflow
The Request VM on CNV workflow is a workflow that demonstrates the following features of the serverless workflow technology:
* Integration with external service, in this case, Jira Cloud and OCP cluster via their OpenAPI 
* Conditional branching
* Using the Notifications plugin to send notifications to the user

The workflow creates a Jira issue and waits for its completion.
After creating the Jira issue, the workflow sends a notification to the default user to be aware of the issue.
While the issue is not resolved, the workflow polls the issue to check its status.
* If the creation is granted, a `VirtualMachines` resource is createdunder the provided namespace.
Then, the workflow is checking the status of the VM for a given amount of time. Once the maximum retries amount is reached or the VM ready, a notification with the status is sent.
* If the creation is denied, a notification is sent.

## Input
- `Jira Project Key` [required] - the Jira Project Key to which the workflow is configured to work and has permission to create and update and issue of type Task.
- `VM name` [required] - The name of the VM to create
- `VM image` [required] - The image to use when creating the VM
- `VM namespace` [required] - The namespace in which create the VM

## Workflow diagram
![Request VM on CNV diagram](https://github.com/parodos-dev/serverless-workflow/blob/main/vm-creator/vm-creator.svg?raw=true)
