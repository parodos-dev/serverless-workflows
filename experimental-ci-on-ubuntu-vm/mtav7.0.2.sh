#!/bin/bash

set -x
set -e

# Standup the cluster and install janus and konveyor operator
./cluster-up.sh
./janus-idp.sh
./koveyor-operator-0.3.2.sh

cd ..

# Create and push MTA image, generate manifests
sudo rm -rf ~/workdir
make WORKFLOW_ID=mtav7.0.2 REGISTRY_REPO=rhkp GIT_USER_NAME=rhkp LOCAL_TEST=true for-local-tests

# Load workflow image
docker save quay.io/rhkp/serverless-workflow-mtav7.0.2:latest -o serverless-workflow-mtav7.0.2.tar
# minikube image load image-archive serverless-workflow-mtav7.0.2.tar
kind load image-archive serverless-workflow-mtav7.0.2.tar

# Copy generated manifests
rm -rf ./manifests
mkdir ./manifests
cp -r ~/workdir/mtav7.0.2/manifests .

# Set the endpoint to the tackle-ui service
yq --inplace '.spec.podTemplate.container.env |= ( . + [{"name": "QUARKUS_REST_CLIENT_MTA_JSON_URL", "value": "http://tackle-ui.my-konveyor-operator.svc:8080"}, {"name": "MTA_HUB_TOKEN", "value": "TEST_TOKEN_VALUE"}, {"name": "BACKSTAGE_NOTIFICATIONS_URL", "value": "http://janus-idp-workflows-backstage.default.svc.cluster.local:7007/api/notifications/"}] )' manifests/01-sonataflow_mta-analysis.yaml

# Disable persistence for e2e tests
yq e '.spec.persistence = {}' -i manifests/01-sonataflow_mta-analysis.yaml
sed -i '/quarkus\.flyway\.migrate-at-start=true/d' manifests/03-configmap_mta-analysis-props.yaml

echo "manifests/03-configmap_mta-analysis-props.yaml"
cat manifests/03-configmap_mta-analysis-props.yaml
echo "---"

echo "manifests/01-sonataflow_mta-analysis.yaml"
cat manifests/01-sonataflow_mta-analysis.yaml
echo "---"

# deploy the manifests created by the ${{ steps.build-image.outputs.image }} image
kubectl apply -f manifests/
sleep 5
kubectl get deployment mta-analysis -o jsonpath={.spec.template.spec.containers[]}
# give the pod time to start
sleep 15
kubectl get pods -o wide
kubectl wait --for=condition=Ready=true pods -l "app=mta-analysis" --timeout=10m

# Run the end to end test
./e2e/mtav7.0.2.sh