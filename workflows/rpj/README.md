# RPJ - Report portal to Jira
# Synopsis
This workflow is an infrastructure workflow type, that invokes a [RPJ][1] run for a specific launch.


# Inputs
- `launchID` [mandatory] - The report portal launch ID to use to generate the Jira task
- `epicCode` - The epic under which the task shall be created
- `recipients` [mandatory] - A list of recipients for the notification in the format of `user:<namespace>/<username>` or `group:<namespace>/<groupname>`, i.e. `user:default/jsmith`.

# Output
1. On completion the workflow returns the run response in a notification.

# Pre-requisites
Access to the RPJ tool directly or via a proxy.

In this project, a side proxy application was created in order to avoid self-signed certificates error when sending REST requests.
If you RPJ is configured properly (i.e: without self-signed certificates), you can remove the proxy application.

# Workflow Diagram
![rpj workflow diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/rpj/rpj.svg?raw=true)

# Workflow application configuration
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `BACKSTAGE_NOTIFICATIONS_URL`      | The backstage server URL for notifications | ✅ | |
| `NOTIFICATIONS_BEARER_TOKEN`      | The authorization bearer token to use to send notifications | ✅ | |
| `RPJ_URL`  | The URL of the Report Portal to Jira tool, or to the proxy giving access to it | ✅ | |

[1]: https://github.com/abrugaro/rp-jira-sync

