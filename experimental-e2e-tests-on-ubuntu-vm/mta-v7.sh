#!/bin/bash

set -x
set -e

# Standup the cluster and install janus and konveyor operator
./cluster-up.sh
./sonata-flow.sh # Due to GitHub Actions resource constraints just use Sonataflow
./konveyor-operator-0.3.2.sh

cd ..

# Create and push MTA image, generate manifests
sudo rm -rf ~/workdir
make WORKFLOW_ID="$WORKFLOW_ID" REGISTRY_REPO="$REGISTRY_REPO" LOCAL_TEST=true for-local-tests

# Load workflow image
docker save quay.io/rhkp/serverless-workflow-"$WORKFLOW_ID":latest -o serverless-workflow-"$WORKFLOW_ID".tar

if [ "$CLUSTER_TYPE" == "minikube" ]; then
    minikube image load serverless-workflow-"$WORKFLOW_ID".tar
else
    kind load image-archive serverless-workflow-"$WORKFLOW_ID".tar
fi

# Copy generated manifests
rm -rf ./manifests
mkdir ./manifests
cp -r ~/workdir/"$WORKFLOW_ID"/manifests .

# Set the endpoint to the tackle-ui service
yq --inplace '.spec.podTemplate.container.env |= ( . + [{"name": "QUARKUS_REST_CLIENT_MTA_JSON_URL", "value": "http://tackle-ui.my-konveyor-operator.svc:8080"}, {"name": "BACKSTAGE_NOTIFICATIONS_URL", "value": "http://janus-idp-workflows-backstage.default.svc.cluster.local:7007/api/notifications/"}] )' manifests/04-sonataflow_mta-analysis-v7.yaml

# Disable persistence for e2e tests
yq e '.spec.persistence = {}' -i manifests/04-sonataflow_mta-analysis-v7.yaml
sed -i '/quarkus\.flyway\.migrate-at-start=true/d' manifests/03-configmap_mta-analysis-v7-props.yaml

echo "manifests/03-configmap_mta-analysis-v7-props.yaml"
cat manifests/03-configmap_mta-analysis-v7-props.yaml
echo "---"

echo "manifests/04-sonataflow_mta-analysis-v7.yaml"
cat manifests/04-sonataflow_mta-analysis-v7.yaml
echo "---"

# deploy the manifests created by the ${{ steps.build-image.outputs.image }} image
kubectl apply -f manifests/
sleep 5
kubectl get deployment mta-analysis-v7 -o jsonpath='{.spec.template.spec.containers[]}'
# give the pod time to start
sleep 15
kubectl get pods -o wide
kubectl wait --for=condition=Ready=true pods -l "app=mta-analysis-v7" --timeout=10m

# Run the end to end test
./e2e/mta-v7.x-vm.sh