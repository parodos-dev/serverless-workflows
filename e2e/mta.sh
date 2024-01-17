#!/bin/bash

set -ex

export port_forward_id=""

function cleanup() {
    echo "cleanup"
    kill -9 "$port_forward_id"
}

function workflowDone() {
    if [[ -n "${1}" ]]; then 
        curl -s "Content-Type: application/json" localhost:9080/MTAAnalysis/"$1"
    fi
}
trap 'cleanup' EXIT SIGTERM

echo "Proxy MTA Analysis sonata port ⏳"
kubectl port-forward svc/mtaanalysis 9080:80 &
port_forward_id="$!"
sleep 1
echo "Proxy MTA Analysis sonata port ✅"

echo "End to end tests start ⏳"
id=$(curl -s -XPOST -H "Content-Type: application/json" localhost:9080/MTAAnalysis -d '{"repositoryURL": "https://github.com/spring-projects/spring-petclinic"}' | jq .id)

retries=3
until eval "test $retries -eq 0 || workflowDone $id"; do
  echo "checking workflow $id is done"
  sleep 3
  retries=$((retries-1))
done

echo "End to end tests passed ✅"
exit 0


