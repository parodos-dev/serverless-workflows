#!/bin/bash

WORKFLOW_ID=$1

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


# Find the workflow file with .sw.yaml suffix since kn-cli uses the ID to generate resource names
workflow_file=$(printf '%s\n' ./*.sw.yaml 2>/dev/null | head -n 1)

# Check if the workflow_file was found
if [ -z "$workflow_file" ]; then
  echo "No workflow file with .sw.yaml suffix found."
  exit 1
fi

# Extract the 'id' property from the YAML file and convert to lowercase
workflow_id=$(grep '^id:' "$workflow_file" | awk '{print $2}' | tr '[:upper:]' '[:lower:]')

# Check if the 'id' property was found
if [ -z "$workflow_id" ]; then
  echo "No 'id' property found in the workflow file."
  exit 1
fi

SONATAFLOW_CR=manifests/01-sonataflow_${workflow_id}.yaml
yq --inplace eval '.metadata.annotations["sonataflow.org/profile"] = "prod"' "${SONATAFLOW_CR}"

yq --inplace ".spec.podTemplate.container.image=\"quay.io/orchestrator/serverless-workflow-${workflow_id}:latest\"" "${SONATAFLOW_CR}"

if test -f "secret.properties"; then
  if [ ! -f kubectl ]; then
    echo "Installing kubectl CLI"
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" 
    chmod +x kubectl
  else 
    echo "kubectl cli already available"
  fi

  yq --inplace ".spec.podTemplate.container.envFrom=[{\"secretRef\": { \"name\": \"${workflow_id}-creds\"}}]" "${SONATAFLOW_CR}"
  ../kubectl create -n sonataflow-infra secret generic "${workflow_id}-creds" --from-env-file=secret.properties --dry-run=client -oyaml > "manifests/01-secret_${workflow_id}.yaml"
fi

if [ "${ENABLE_PERSISTENCE}" = true ]; then
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
fi
