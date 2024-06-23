#!/bin/bash

WORKFLOW_ID=$1

ENABLE_PERSISTENCE=false
if [ "$2" = "true" ]; then
    ENABLE_PERSISTENCE=true
fi

if [ ! -f kn ]; then
  echo "Installing kn-workflow CLI"
  KN_CLI_URL="https://mirror.openshift.com/pub/openshift-v4/clients/serverless/1.11.2/kn-linux-amd64.tar.gz"
  curl -L "$KN_CLI_URL" | tar -xz --no-same-owner && chmod +x kn-linux-amd64 && mv kn-linux-amd64 kn
else 
  echo "kn cli already available"
fi

cd "${WORKFLOW_ID}" || exit

echo -e "\nquarkus.flyway.migrate-at-start=true" >> application.properties

# TODO Update to use --skip-namespace when the following is released
# https://github.com/apache/incubator-kie-tools/pull/2136
../kn workflow gen-manifest --namespace ""


if [ "$ENABLE_PERSISTENCE" = false ]; then
  exit
fi

SONATAFLOW_CR=manifests/01-sonataflow_${WORKFLOW_ID}.yaml
yq --inplace eval '.metadata.annotations["sonataflow.org/profile"] = "prod"' "${SONATAFLOW_CR}"

yq --inplace ".spec.podTemplate.container.image=\"quay.io/orchestrator/serverless-workflow-${WORKFLOW_ID}:latest\"" "${SONATAFLOW_CR}"

yq --inplace ".spec |= (
  . + {
    \"persistence\": {
      \"postgresql\": {
        \"secretRef\": {
          \"name\": \"sonataflow-psql-postgresql\",
          \"userKey\": \"postgres-username\",
          \"passwordKey\": \"postgres-password\"
        },
        \"serviceRef\": {
          \"name\": \"sonataflow-psql-postgresql\",
          \"port\": 5432,
          \"databaseName\": \"sonataflow\",
          \"databaseSchema\": \"${WORKFLOW_ID}\"
        }
      }
    }
  }
)" "${SONATAFLOW_CR}"