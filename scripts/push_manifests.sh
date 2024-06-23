#!/bin/bash -x

GIT_USER_NAME=$1
GIT_USER_EMAIL=$2
GIT_TOKEN=$3
PR_OR_COMMIT_URL=$4
DEPLOYMENT_REPO=$5
DEPLOYMENT_BRANCH=$6
WORKFLOW_ID=$7
APPLICATION_ID=$8
IMAGE_NAME=$9
# Don't use $10 or it would be interpreted as the string "${1}0"
IMAGE_TAG=${10}

echo "Cloning ${DEPLOYMENT_REPO}"
git clone https://github.com/"${DEPLOYMENT_REPO}" helm-repo
cd helm-repo || exit

git switch -c autopr-${RANDOM} origin/"${DEPLOYMENT_BRANCH}" origin/staging
# We assume the kustomize project already exists, this is not part of the PR
cp ../"${WORKFLOW_ID}"/manifests/* kustomize/"${WORKFLOW_ID}"/base
# Applying image kustomization
cd kustomize/"${WORKFLOW_ID}"/base || exit
if [ -n "$APPLICATION_ID" ] && [ "$APPLICATION_ID" != "UNDEFINED" ]; then
  kustomize edit set image serverless-workflow-"${APPLICATION_ID}"="${IMAGE_NAME}":"${IMAGE_TAG}"
else
  kustomize edit set image serverless-workflow-"${WORKFLOW_ID}"="${IMAGE_NAME}":"${IMAGE_TAG}"
fi
git diff
git add -A

git config --global user.email "${GIT_USER_EMAIL}"
git config --global user.name "${GIT_USER_NAME}"

git commit -m "Automated PR from ${PR_OR_COMMIT_URL}"
git remote set-url origin https://"${GIT_TOKEN}"@github.com/"${DEPLOYMENT_REPO}"
git push origin HEAD
