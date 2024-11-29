# VM Updater workflow
The VM updater workflow is a workflow that demonstrates the following features of the serverless workflow technology:
* Integration with external service, in this case, OCP cluster via its OpenAPI 
* Conditional branching
* Using the Notifications plugin to send notifications to the user

## Prequisites
* Having the openshift-cnv operator installed and running

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
- `VM namespace` [required] - The namespace in which create the VM
- `Memory` - The new guest memory of the VM
- `CPU cores` - The new amount of CPU cores available to the VM
- `CPU threads` - The new amount of CPU threads available to the VM
- `CPU sockets` - The new amount of CPU sockets available to the VM
- `Auto restart VM` - Auto restart the VM to put into effect the changes
- `Recipients` [mandatory] - A list of recipients for the notification in the format of `user:<namespace>/<username>` or `group:<namespace>/<groupname>`, i.e. `user:default/jsmith`.

## Workflow diagram
![VM Updater diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/modify-vm-resources/modify-vm-resources.svg?raw=true)

## Installation

See [official installation guide](https://github.com/parodos-dev/serverless-workflows-config/blob/main/docs/main/modify-vm-resources)