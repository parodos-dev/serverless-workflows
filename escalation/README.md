# Simple escalation workflow
An escalation workflow integrated with Atlassian JIRA using [SonataFlow](https://sonataflow.org/serverlessworkflow/latest/index.html).

## Prerequisite
* Access to a Jira server (URL, user and [API token](https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/))
* Access to an OpenShift cluster with `admin` Role

## Workflow diagram
![Escalation workflow diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/escalation/ticketEscalation.svg?raw=true)

**Note**:
The value of the `.jiraIssue.fields.status.statusCategory.key` field is the one to be used to identify when the `done` status is reached, all the other
similar fields are subject to translation to the configured language and cannot be used for a consistent check.

## Application configuration
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `JIRA_URL`            | The Jira server URL | ✅ | |
| `JIRA_USERNAME`       | The Jira server username | ✅ | |
| `JIRA_API_TOKEN`      | The Jira API Token | ✅ | |
| `JIRA_PROJECT`        | The key of the Jira project where the escalation issue is created | ❌ | `TEST` |
| `JIRA_ISSUE_TYPE`     | The ID of the Jira issue type to be created | ✅ | |
| `OCP_API_SERVER_URL`  | The OpensShift API Server URL | ✅ | |
| `OCP_API_SERVER_TOKEN`| The OpensShift API Server Token | ✅ | |
| `ESCALATION_TIMEOUT_SECONDS` | The number of seconds to wait before triggering the escalation request, after the issue has been created | ❌ | `60` |
| `POLLING_PERIODICITY`(1) | The polling periodicity of the issue state checker, according to ISO 8601 duration format | ❌ | `PT6S` |

(1) This is still hardcoded as `PT5S` while waiting for a fix to [KOGITO-9811](https://issues.redhat.com/browse/KOGITO-9811)
## How to run

```bash
mvn clean quarkus:dev
```

Example of POST to trigger the flow (see input schema in [ocp-onboarding-schema.json](./src/main/resources/ocp-onboarding-schema.json)):
```bash
curl -XPOST -H "Content-Type: application/json" http://localhost:8080/ticket-escalation -d '{"namespace": "_YOUR_NAMESPACE_"}'
```

Tips:
* Visit [Workflow Instances](http://localhost:8080/q/dev/org.kie.kogito.kogito-quarkus-serverless-workflow-devui/workflowInstances)
* Visit (Data Index Query Service)[http://localhost:8080/q/graphql-ui/]
