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
kubectl expose "$(kubectl get pod -o name | grep mta-analysis)" --type="NodePort" --port=8080 --name=mta-svc
kubectl port-forward svc/mta-svc 8080:8080 &
port_forward_pid="$!"
sleep 3
echo "Proxy MTA Analysis port ✅"

echo "End to end tests start ⏳"

resp=$(curl --location 'http://localhost:8080/mta-analysis' --header 'Accept: application/json, text/plain, */*' --header 'Content-Type: application/json' --data '{ "repositoryURL": "https://github.com/spring-projects/spring-petclinic", "exportToIssueManager": "false", "migrationStartDatetime" : "2024-07-01T00:00:00Z", "migrationEndDatetime" : "2024-07-31T00:00:00Z"}')

id=$(echo "$resp" | jq ".id")

if [ -z "$id" ] || [ "$id" == "null" ]; then
    echo "workflow instance id is null... exiting "
    echo "Debug Info Start - MTA 7.0.2 Workflow"
    kubectl logs "$(kubectl get pod -o name | grep mta-analysis)"
    echo "Debug Info End - MTA 7.0.2 Workflow"

    echo "Debug Info Start - Jobs Service"
    kubectl logs "$(kubectl get pod -o name -n sonataflow-infra | grep sonataflow-platform-jobs-service)" -n sonataflow-infra
    echo "Debug Info End - Jobs Service"

    exit 1
fi

mta_analysis_pod=$(kubectl get pod -o name | grep mta-analysis)
retries=20
until eval "test ${retries} -eq 0"; do
  analysis_out=$(kubectl logs "$mta_analysis_pod")
  success_result="\"mtaAnalysisResultURL\" : \"http://tackle-ui.my-konveyor-operator.svc.cluster.local:8080/hub/applications"
  if grep -q "${success_result}" <<< "$analysis_out"
  then
    echo "End to end tests passed ✅"
    exit 0
  else
    echo "checking workflow ${id} completed successfully"
    sleep 30
    retries=$((retries-1))
  fi
done

echo "Analysis did not finish in stipulated time ❌"