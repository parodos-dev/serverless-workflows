#!/bin/bash

set -x
set -e

# holds the pid of the port forward process for cleanups
export port_forward_pid=""

function cleanup() {
    echo "cleanup $?"
    kill "$port_forward_pid" || true
    kill "$move2kube_port_forward_pid" || true
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

echo "Proxy move2kube instance port ⏳"
kubectl port-forward  svc/move2kube-instance-svc 8080:8080 &
move2kube_port_forward_pid="$!"
sleep 3
echo "Proxy move2kube instance port ✅"


echo "End to end tests start ⏳"
MOVE2KUBE_URL="http://localhost:8080"
BACKSTAGE_URL="http://localhost:9080"
BACKSTAGE_NOTIFICATION_URL="${BACKSTAGE_URL}/api/notifications/"
GIT_ORG="gfarache31/m2k-test"
GIT_REPO="bitbucket.org/${GIT_ORG}"
GIT_SOURCE_BRANCH="master"
GIT_TARGET_BRANCH="e2e-test-$(date +%s)"
echo "Creating workspace and project in move2kube instance"
WORKSPACE_ID=$(curl -X POST "${MOVE2KUBE_URL}/api/v1/workspaces" -H 'Content-Type: application/json' --data '{"name": "e2e Workspace",  "description": "e2e tests"}' | jq -r .id)
PROJECT_ID=$(curl -X POST "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects" -H 'Content-Type: application/json' --data '{"name": "e2e Project",  "description": "e2e tests"}' | jq -r .id)

echo "Wait until M2K workflow is available in backstage..."
M2K_STATUS=$(curl -XGET -s -o /dev/null -w "%{http_code}" ${BACKSTAGE_URL}/api/orchestrator/workflows/m2k)
until [ "$M2K_STATUS" -eq 200 ]
do
sleep 5
M2K_STATUS=$(curl -XGET -s -o /dev/null -w "%{http_code}" ${BACKSTAGE_URL}/api/orchestrator/workflows/m2k)
done

echo "M2K is available in backstage, sending execution request"
out=$(curl -XPOST -H "Content-Type: application/json"  ${BACKSTAGE_URL}/api/orchestrator/workflows/m2k/execute -d "{\"repositoryURL\": \"ssh://${GIT_REPO}\", \"sourceBranch\": \"${GIT_SOURCE_BRANCH}\", \"targetBranch\": \"${GIT_TARGET_BRANCH}\", \"workspaceId\": \"${WORKSPACE_ID}\", \"projectId\": \"${PROJECT_ID}\"}")
id=$(echo "$out" | jq -e .id)

if [ -z "$id" ] || [ "$id" == "null" ]; then
    echo "workflow instance id is null... exiting "
    exit 1
fi


echo "Wait until plan exists"
retries=20
http_status=$(curl -X GET -s -o /dev/null -w "%{http_code}" "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}/plan")
while [ ${retries} -ne 0 ] && [ "${http_status}" -eq 404 ]; do
echo "Wait until plan exists"
  sleep 5
  retries=$((retries-1))
  http_status=$(curl -X GET -s -o /dev/null -w "%{http_code}" "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}/plan")
done

if [ "${http_status}" -eq 204 ]
then
  echo "Plan not created, error when creating it, checks move2kbe logs, http status=${http_status}...exiting "
  exit 1
fi

if [ "${http_status}" -eq 404 ]
then
  echo "Plan not created, http status=${http_status}...exiting "
  exit 1
fi

echo "Checking if Q&A waiting notification with move2kube URL received"
NOTIFICATION=$(curl "${BACKSTAGE_NOTIFICATION_URL}/notifications?messageScope=all&orderBy=created&orderByDirec=desc" | jq ".[0]")
URL_IN_NOTIFICATION=$(printf "%s" "${NOTIFICATION}" | jq ".actions[0].url | select(contains(\"${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}/outputs\"))")
if [ -z "${URL_IN_NOTIFICATION}" ]
then
      printf "Notification has no action with matching URL: %s\n\nexiting " "${NOTIFICATION}"
      exit 1
fi

echo "Checking if Knative function running"
nb_pods=$(kubectl get pods -l app=m2k-save-transformation-func-v1 -no-headers | wc -l)
retries=20
while [[ ${retries} -ne 0 && ${nb_pods} -eq 0 ]]; do
echo "Wait until Knative function running"
  sleep 5
  retries=$((retries-1))
  nb_pods=$(kubectl get pods -l app=m2k-save-transformation-func-v1 --no-headers | wc -l)
done

if [[ $nb_pods -ne 1 ]]
then
  echo "Knative function not running...exiting "
  exit 1
fi

echo "Answering Q&A to continue workflow"
TRANSFORMATION_ID=$(curl "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}" | jq -r '.outputs | keys'[0])
current_question=$(curl -X GET "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}/outputs/${TRANSFORMATION_ID}/problems/current")
question_id=$(echo "${current_question}" | jq -r '.question | fromjson | .id' | sed -r -e 's/"/\\\\\\\"/g')
default_answer=$(echo "${current_question}" | jq '.question | fromjson | .default' | sed -r -e 's/"/\\"/g' | tr '\n' ' ')
while [ "${question_id}" != "" ]; do
  curl -iX POST "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}/outputs/${TRANSFORMATION_ID}/problems/current/solution" \
       -H 'Content-Type: application/json' \
       -d "{\"solution\": \"{\\\"id\\\":\\\"${question_id}\\\",\\\"answer\\\":${default_answer}}\"}"
  current_question=$(curl -X GET "${MOVE2KUBE_URL}/api/v1/workspaces/${WORKSPACE_ID}/projects/${PROJECT_ID}/outputs/${TRANSFORMATION_ID}/problems/current")
  question_id=$(echo "${current_question}" | jq -r '.question | fromjson | .id' | sed -r -e 's/"/\\\\\\\"/g')
  default_answer=$(echo "${current_question}" | jq '.question | fromjson | .default' | sed -r -e 's/"/\\"/g' | tr '\n' ' ')
done

echo "Checking if branch ${GIT_TARGET_BRANCH} created on git repo ${GIT_REPO}"

http_status=$(curl -X GET -L -s -o /dev/null -w "%{http_code}" "https://api.bitbucket.org/2.0/repositories/${GIT_ORG}/refs/branches/${GIT_TARGET_BRANCH}")
retries=20
while [[ ${retries} -ne 0 && ${http_status} -eq 404 ]]; do
  sleep 5
  retries=$((retries-1))
http_status=$(curl -X GET -L -s -o /dev/null -w "%{http_code}" "https://api.bitbucket.org/2.0/repositories/${GIT_ORG}/refs/branches/${GIT_TARGET_BRANCH}")
done
if [ "${http_status}" -eq 404 ]
then
  echo "Branch ${GIT_TARGET_BRANCH} not created on repo ${GIT_REPO}...exiting "
  exit 1
else
  echo "Branch ${GIT_TARGET_BRANCH} successfully created on repo ${GIT_REPO}! "
fi

echo "Checking if completion notification received"
NOTIFICATION=$(curl "${BACKSTAGE_NOTIFICATION_URL}/notifications?messageScope=all&orderBy=created&orderByDirec=desc" | jq ".[0]")
SUCCESS_NOTIFICATION=$(printf "%s" "${NOTIFICATION}" | jq ".message | select(contains(\"success\"))")
if [ -z "${SUCCESS_NOTIFICATION}" ]
then
      printf "Notification has no action with matching URL: %s\n\nexiting " "${NOTIFICATION}"
      exit 1
fi

echo "End to end tests passed ✅"
