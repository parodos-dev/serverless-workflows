# RPJ - Report portal to Jira
# Synopsis
This workflow is an infrastructure workflow type, that invokes a [RPJ][1] run for a specific launch.


# Inputs
- `launchID` [mandatory] - The report portal launch ID to use to generate the Jira task
- `epicCode` - The epic under which the task shall be created
- `recipients` [mandatory] - A list of recipients for the notification in the format of `user:<namespace>/<username>` or `group:<namespace>/<groupname>`, i.e. `user:default/jsmith`.

# Output
1. On completion the workflow returns the run response in a notification.

# Dependencies
- RPJ tool

# Runtime configuration

| key                                                  | default                                                                                      | description                               |
|------------------------------------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------|
| rpj.url                                              |                                      | Endpoint (with protocol and port) for RPJ |
| quarkus.rest-client.rpj_json.url                     | ${rpj.url}                           | RPJ api                               |
| quarkus.rest-client.notifications.url                | ${BACKSTAGE_NOTIFICATIONS_URL:http://backstage-backstage.rhdh-operator/api/notifications/} | Backstage notification url                |

All the configuration items are on [./application.properties]

# Testing
TODO

# Workflow Diagram
![rpj workflow diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/rpj/rpj.svg?raw=true)

[1]: https://github.com/abrugaro/rp-jira-sync
