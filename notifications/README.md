# Application Onboarding workflow
The application onboarding workflow is a workflow that demonstrates the following features of the serverless workflow technology:
* Integration with external service, in this case Jira Cloud via its OpenAPI
* Callback events
* Timeout of a workflow
* Conditional branching
* Using the Notifications plugin to send notifications to the user

The workflow creates a Jira issue and waits for its completion within 30s.
After creating the Jira issue, the workflow sends notification to the default user to be aware of the issue.
If the Jira is resolved within 30s, the workflow sends a notification that the issue is resolved.
If the Jira issue isn't resolved within 30s, the workflow closes it and sends a notification the workflow was closed due to a timeout.

This workflow can be extended to introduce more capabilities, such as creating a Namespace in OpenShift cluster for the user and deploy a given application to that namespace. This is excluded from the current workflow for simplifying it.

## Input
- `Jira Project Key` [required] - the Jira Project Key to which the workflow is configured to work with and has permission to create and update and issue of type Task.

## Workflow diagram
![Application Onboarding diagram](https://github.com/parodos-dev/serverless-workflow-examples/blob/main/application-onboarding/application-onboarding.svg?raw=true)