# MTV migration workflow - MTV Migration execution workflow
This workflow is a continuation of the MTV assessment workflow. It executes an MTV Plan and waits for its final condition. Final condition is either success or failure. It is important to note that we rely on MTV to reach a final state. We do not impose our own timeout.  
[MTV Migration Plan documentation](https://docs.redhat.com/en/documentation/migration_toolkit_for_virtualization/2.6/html/installing_and_using_the_migration_toolkit_for_virtualization/migrating-vms-web-console_mtv#creating-migration-plans-ui)

## Prerequisite
* Access to an OCP cluster with MTV operator (Openshift Migration Toolkit for Virtualization) installed. The cluster credentials must allow creating the resources listed above.

## Workflow diagram
![MTV Migration workflow diagram](https://github.com/parodos-dev/serverless-workflows/blob/main/mtv-migration/mtv.svg?raw=true)

## Workflow application configuration
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory | Default value |
|-----------------------|-------------|-----------|---------------|
| `BACKSTAGE_NOTIFICATIONS_URL`      | The backstage server URL for notifications | ✅ | |
| `NOTIFICATIONS_BEARER_TOKEN`      | The authorization bearer token to use to send notifications | ✅ | |
| `OCP_API_SERVER_URL`  | The OpensShift API Server URL | ✅ | |
| `OCP_API_SERVER_TOKEN`| The OpensShift API Server Token | ✅ | |

## Installation

See [official installation guide](https://github.com/parodos-dev/serverless-workflows-config/blob/main/docs/main/mtv-migration)

## How to run
Example of POST to trigger the flow (see input schema [mtv-input.json](./schema/mtv-input.json)):
```bash
curl -X POST -H "Content-Type: application/json" http://localhost:8080/mtv-migration -d '{
    "migrationName": "my-vms",
    "migrationNamespace": "openshift-mtv"
}'
```
