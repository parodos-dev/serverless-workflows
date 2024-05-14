#!/bin/bash

set -x
set -e

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
