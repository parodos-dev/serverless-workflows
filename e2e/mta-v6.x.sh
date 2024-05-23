#!/bin/bash

set -x
set -e

# holds the pid of the port forward process for cleanups
export port_forward_pid=""

function cleanup() {
    echo "cleanup $?"
    kill "$port_forward_pid" || true
}

function workflowDone() {
    if [[ -n "${1}" ]]; then 
        id=$1
        curl -s -H "Content-Type: application/json" localhost:9080/api/orchestrator/instances/"${id}" | jq -e '.state == "COMPLETED"'
    fi
}

trap 'cleanup' EXIT SIGTERM

echo "Proxy Janus-idp port ⏳"
kubectl port-forward "$(kubectl get svc -l app.kubernetes.io/component=backstage -o name)" 9080:7007 &
port_forward_pid="$!"
sleep 3
echo "Proxy Janus-idp port ✅"

echo "End to end tests start ⏳"

out=$(curl -XPOST -H "Content-Type: application/json"  http://localhost:9080/api/orchestrator/workflows/mta-analysis-v6/execute -d '{"repositoryURL": "https://github.com/spring-projects/spring-petclinic", "exportToIssueManager": "false", "migrationStartDatetime" : "2024-07-01T00:00:00Z", "migrationEndDatetime" : "2024-07-31T00:00:00Z"}')
id=$(echo "$out" | jq -e .id)

if [ -z "$id" ] || [ "$id" == "null" ]; then
    echo "workflow instance id is null... exiting "
    exit 1
fi

retries=20
until eval "test ${retries} -eq 0 || workflowDone $id"; do
  echo "checking workflow ${id} completed successfully"
  sleep 5
  retries=$((retries-1))
done

echo "End to end tests passed ✅"

