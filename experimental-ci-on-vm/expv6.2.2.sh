#!/bin/bash

set -x
set -e

kind delete cluster
kind create cluster

kubectl apply -f https://raw.githubusercontent.com/operator-framework/operator-lifecycle-manager/master/deploy/upstream/quickstart/crds.yaml
# give the apiserver time
sleep 5
kubectl apply -f https://raw.githubusercontent.com/operator-framework/operator-lifecycle-manager/master/deploy/upstream/quickstart/olm.yaml

# install konveyor operator
# version 0.2 is MTA 6.2 and 0.3 is 7.x
kubectl create -f https://operatorhub.io/install/konveyor-0.2/konveyor-operator.yaml
# give the apiserver time
echo "sleeping 300 seconds to give time for the operator to pull images and start"
sleep 300
kubectl get csv -A 
# TODO its a bit smelly that the csv name is coded here. 
kubectl wait --for=jsonpath='{.status.phase}=Succeeded' -n my-konveyor-operator csv/konveyor-operator.v0.2.1 
kubectl get pods -A
kubectl wait --for=condition=Ready=true pods -l "name=tackle-operator" -n my-konveyor-operator --timeout=240s
kubectl get crds

# Tackle creation
kubectl create -f - << EOF
kind: Tackle
apiVersion: tackle.konveyor.io/v1alpha1
metadata:
  name: tackle
  namespace: my-konveyor-operator
spec:
  feature_auth_required: false
  hub_database_volume_size: 1Gi
  hub_bucket_volume_size: 1Gi
EOF

kubectl get pods -n my-konveyor-operator
sleep 60
kubectl get tackle -n my-konveyor-operator -o yaml
echo "wait for tackle ui to be ready"
kubectl get pods -n my-konveyor-operator
sleep 300
kubectl wait --for=condition=Ready=true pods -l "app.kubernetes.io/name=tackle-ui" -n my-konveyor-operator --timeout=240s

# Install JanusIDP
helm repo add janus-idp-workflows https://rgolangh.github.io/janus-idp-workflows-helm/
helm install janus-idp-workflows janus-idp-workflows/janus-idp-workflows \
--set backstage.upstream.backstage.image.tag=1.1 \
-f https://raw.githubusercontent.com/rgolangh/janus-idp-workflows-helm/main/charts/kubernetes/orchestrator/values-k8s.yaml

echo "sleep bit long till the PV for data index and kaniko cache is ready. its a bit slow. TODO fixit"
kubectl get pv
sleep 180

kubectl get sfp -A
kubectl wait --for=condition=Ready=true pods -l "app.kubernetes.io/name=backstage" --timeout=600s
kubectl get pods -o wide
kubectl wait --for=condition=Ready=true pods -l "app=sonataflow-platform" --timeout=600s

cd ..

# Create and push image, generate manifests
sudo rm -rf ~/workdir
make WORKFLOW_ID=mtav6.2.2 REGISTRY_REPO=rhkp GIT_USER_NAME=rhkp LOCAL_TEST=true for-local-tests

# Load workflow image
docker save quay.io/rhkp/serverless-workflow-mtav6.2.2:latest -o serverless-workflow-mtav6.2.2.tar
# minikube image load image-archive serverless-workflow-mtav6.2.2.tar
kind load image-archive serverless-workflow-mtav6.2.2.tar

# Copy generated manifests
rm -rf ./manifests
mkdir ./manifests
cp -r ~/workdir/mtav6.2.2/manifests .

# Set the endpoint to the tackle-ui service
yq --inplace '.spec.podTemplate.container.env |= ( . + [{"name": "QUARKUS_REST_CLIENT_MTA_JSON_URL", "value": "http://tackle-ui.my-konveyor-operator.svc:8080"}, {"name": "MTA_HUB_TOKEN", "value": "???TBD???"}, {"name": "BACKSTAGE_NOTIFICATIONS_URL", "value": "http://janus-idp-workflows-backstage.default.svc.cluster.local:7007/api/notifications/"}] )' manifests/01-sonataflow_mta-analysis.yaml

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