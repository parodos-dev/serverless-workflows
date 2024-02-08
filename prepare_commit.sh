#!/bin/bash

USER_NAME=$1
USER_EMAIL=$2
WF_HELM_REPO=$3
WORKFLOW_ID=$4
IMAGE_NAME=$5
IMAGE_TAG=$6

RANDOM=$(date +%s | sha256sum | base64 | head -c 5)

# Install tar and git (needed for ubi9-minimal)
# microdnf install -y git tar
# microdnf clean all

# Install kustomize
# curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
# mv kustomize /usr/local/bin

git config --global user.email "${USER_EMAIL}"
git config --global user.name "${USER_NAME}"

git config -l

echo "Cloning ${WF_HELM_REPO}"
git clone ${WF_HELM_REPO} helm-repo
ls
cd helm-repo

# TODO Remove start branch
git switch -c autopr-${RANDOM} origin/FLPATH-957
# We assume the kustomize project already exists, this is not part of the PR
cp ../${WORKFLOW_ID}/manifests/* kustomize/${WORKFLOW_ID}/base
# Applying image kustomization
cd kustomize/${WORKFLOW_ID}/overlays/prod
kustomize edit set image serverless-workflow-${WORKFLOW_ID}=${IMAGE_NAME}:${IMAGE_TAG}