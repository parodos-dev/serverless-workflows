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

## Workflow application configuration
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `BACKSTAGE_NOTIFICATIONS_URL`      | The backstage server URL for notifications | ✅ | |
| `NOTIFICATIONS_BEARER_TOKEN`      | The authorization bearer token to use to send notifications | ✅ | |
| `JIRA_URL`      | The jira server URL | ✅ | |
| `JIRA_USERNAME`      | The jira username | ✅ | |
| `JIRA_API_TOKEN`      | The jira password | ✅ | |
| `OCP_CONSOLE_URL`   | The OCP Console server url. Will be used for links in notifications | ✅ | |
| `OCP_API_SERVER_URL`      | The OCP API server url | ✅ | |
| `OCP_API_SERVER_TOKEN`      | The authorization bearer token to use when sending request to OCP | ✅ | |
| `VM_CHECK_RUNNING_MAX_RETRIES`      | Amount of retries before considering the VM is not running | ❌ | 10 |


## Input
- `Jira Project Key` [required] - the Jira Project Key to which the workflow is configured to work and has permission to create and update and issue of type Task.
- `VM name` [required] - The name of the VM to create
- `VM image` [required] - The image to use when creating the VM
- `VM namespace` [required] - The namespace in which create the VM
- `Recipients` [mandatory] - A list of recipients for the notification in the format of `user:<namespace>/<username>` or `group:<namespace>/<groupname>`, i.e. `user:default/jsmith`.

## Workflow diagram
![Request VM on CNV diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/request-vm-cnv/request-vm-cnv.svg?raw=true)

## Installation

See [official installation guide](https://github.com/parodos-dev/serverless-workflows-config/blob/main/docs/main/request-vm-cnv)
