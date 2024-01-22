#!/bin/bash

set -e

# holds the pid of the port forward process for cleanups
export port_forward_pid=""

function cleanup() {
    echo "cleanup"
    kill -9 "$port_forward_pid"
}

function workflowDone() {
    if [[ -n "${1}" ]]; then 
        id=$1
        curl -s -H "Content-Type: application/json" localhost:9080/api/orchestrator/instances/${id} | jq -e '.state == "COMPLETED"'
    fi
}

trap 'cleanup' EXIT SIGTERM

echo "Proxy Janus-idp port ⏳"
kubectl port-forward svc/workflows-backstage 9080:7007 &
port_forward_pid="$!"
sleep 1
echo "Proxy Janus-idp port ✅"

echo "End to end tests start ⏳"
id=$(curl -s -XPOST -H "Content-Type: application/json" \
    localhost:9080/api/orchestrator/workflows/MTAAnalysis/execute \
    -d '{"repositoryURL": "https://github.com/spring-projects/spring-petclinic"}' \
    | jq .id)

retries=20
sleepduration=5
until eval "test $retries -eq 0 || workflowDone $id"; do
  echo "checking workflow $id completed successfully"
  sleep 5
  retries=$((retries-1))
done

echo "End to end tests passed ✅"
exit 0

