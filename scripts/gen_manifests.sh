#!/bin/bash

WORKFLOW_ID=$1

if [ ! -f kn-workflow ]; then
  echo "Installing kn-workflow CLI"
  # TODO Update to released version
  curl -L https://github.com/rgolangh/kie-tools/releases/download/0.0.2/kn-workflow-linux-amd64 -o kn-workflow
  chmod +x kn-workflow
else 
  echo "kn-workflow already available"
fi

cd "${WORKFLOW_ID}" || exit
# TODO Update to use --skip-namespace when the following is released
# https://github.com/apache/incubator-kie-tools/pull/2136
../kn-workflow gen-manifest --namespace ""
