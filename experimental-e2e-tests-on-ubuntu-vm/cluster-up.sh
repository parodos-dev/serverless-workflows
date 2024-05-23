#!/bin/bash

set -x
set -e

if [ "$CLUSTER_TYPE" == "minikube" ]; then
    echo "Using minikube cluster"
    minikube delete
    minikube start
else
    echo "Using kind cluster"
    kind delete cluster
    kind create cluster
fi 

kubectl apply -f https://raw.githubusercontent.com/operator-framework/operator-lifecycle-manager/master/deploy/upstream/quickstart/crds.yaml
# give the apiserver time
sleep 5
kubectl apply -f https://raw.githubusercontent.com/operator-framework/operator-lifecycle-manager/master/deploy/upstream/quickstart/olm.yaml