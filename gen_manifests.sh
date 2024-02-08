#!/bin/bash

WORKFLOW_ID=$1

if [ ! -f kn-workflow ]; then
  curl -L https://github.com/rgolangh/kie-tools/releases/download/0.0.2/kn-workflow-linux-amd64 -o kn-workflow
else 
  echo "Found kn-workflow"
fi
chmod +x kn-workflow
cd ${WORKFLOW_ID}
../kn-workflow gen-manifest --namespace ""
