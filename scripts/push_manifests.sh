#!/bin/bash

GIT_USER_NAME=$1
GIT_USER_EMAIL=$2
GIT_TOKEN=$3
PR_OR_COMMIT_URL=$4
DEPLOYMENT_REPO=$5
WORKFLOW_ID=$6
IMAGE_NAME=$7
IMAGE_TAG=$8

BRANCH_NAME=main

echo "Cloning ${DEPLOYMENT_REPO}"
git clone https://github.com/"${DEPLOYMENT_REPO}" helm-repo
cd helm-repo || exit

git switch -c autopr-${RANDOM} origin/${BRANCH_NAME}
# We assume the kustomize project already exists, this is not part of the PR
cp ../"${WORKFLOW_ID}"/manifests/* kustomize/"${WORKFLOW_ID}"/base
# Applying image kustomization
cd kustomize/"${WORKFLOW_ID}"/overlays/prod || exit
kustomize edit set image serverless-workflow-"${WORKFLOW_ID}"="${IMAGE_NAME}":"${IMAGE_TAG}"
git diff
git add -A

git config --global user.email "${GIT_USER_EMAIL}"
git config --global user.name "${GIT_USER_NAME}"

git commit -m "Automated PR from ${PR_OR_COMMIT_URL}"
git remote set-url origin https://"${GIT_TOKEN}"@github.com/"${DEPLOYMENT_REPO}"
git push origin HEAD
