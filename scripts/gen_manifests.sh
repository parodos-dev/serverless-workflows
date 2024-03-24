#!/bin/bash

WORKFLOW_ID=$1

if [ ! -f kn ]; then
  echo "Installing kn-workflow CLI"
  KN_CLI_URL="https://mirror.openshift.com/pub/openshift-v4/clients/serverless/latest/kn-linux-amd64.tar.gz"
  curl -L "$KN_CLI_URL" | tar -xz --no-same-owner && chmod +x kn-linux-amd64 && mv kn-linux-amd64 kn
else 
  echo "kn cli already available"
fi

cd "${WORKFLOW_ID}" || exit
# TODO Update to use --skip-namespace when the following is released
# https://github.com/apache/incubator-kie-tools/pull/2136
../kn workflow gen-manifest --namespace ""
