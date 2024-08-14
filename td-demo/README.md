# Demo
This workflow launches an Ansible Automation Platform (AAP) job and then create Deployment, Service and Route on a remote OpenShift Cluster.
Notifications are sent to notify for success or failure upon completion.
The following two (2) inputs are required:
- Job template Id
- Inventory group

## Workflow diagram
![Demo workflow diagram](https://github.com/parodos-dev/serverless-workflow/blob/main/td-demo/td-demo.svg?raw=true)

## Prerequisites
* A running instance of AAP with admin credentials. 
* A running instance of Backstage notification plugin.
* An OCP cluster with a ServiceAcount (SA) having permission to create and get Deployment, Service and Route.

## Workflow application configuration
Application properties can be initialized from environment variables before running the application:

| Environment variable  | Description | Mandatory |
|-----------------------|-------------|-----------|
| `AAP_URL`       | The AAP server URL | ✅ |
| `AAP_USERNAME`      | The AAP server password | ✅ |
| `AAP_PASSWORD`      | The AAP server password | ✅ |
| `OCP_API_URL`      | The OpenShift API URL | ✅ |
| `OCP_API_TOKEN`      | The OCP token to perform the creation of Deployment, Service and Route | ✅ |


