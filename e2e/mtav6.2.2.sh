#!/bin/bash

set -x
set -e

# holds the pid of the port forward process for cleanups
export port_forward_pid=""

function cleanup() {
    echo "cleanup $?"
    kill "$port_forward_pid" || true
}

trap 'cleanup' EXIT SIGTERM

echo "Proxy MTA Analysis port ⏳"
kubectl port-forward "$(kubectl get svc mta-analysis -o name)" 8080:80 &
port_forward_pid="$!"
sleep 3
echo "Proxy MTA Analysis port ✅"

echo "End to end tests start ⏳"

resp=$(curl --location 'http://localhost:8080/mta-analysis' --header 'Accept: application/json, text/plain, */*' --header 'Content-Type: application/json' --data '{ "repositoryURL": "https://github.com/spring-projects/spring-petclinic", "exportToIssueManager": "false", "migrationStartDatetime" : "2024-07-01T00:00:00Z", "migrationEndDatetime" : "2024-07-31T00:00:00Z"}')

id=$(echo "$resp" | jq ".id")

if [ -z "$id" ] || [ "$id" == "null" ]; then
    echo "workflow instance id is null... exiting "
    exit 1
fi

mta_analysis_pod=$(kubectl get pod -o name | grep mta-analysis)
retries=20
sleep 30
until eval "test ${retries} -eq 0"; do
  analysis_out=$(kubectl logs $mta_analysis_pod | grep "\"mtaAnalysisResultURL\" : \"http://tackle-ui.my-konveyor-operator.svc.cluster.local:8080/hub/applications")
  if [ -z "$analysis_out" ] || [ "$analysis_out" == "null" ]; then
    echo "checking workflow ${id} completed successfully"
    sleep 5
    retries=$((retries-1))
  else
    echo "End to end tests passed ✅"
    exit 0
  fi
done

echo "Analysis did not finish in stipulated time ❌"

