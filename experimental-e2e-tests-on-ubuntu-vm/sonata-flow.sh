#!/bin/bash

set -x
set -e

# Namespace for Sonataflow
kubectl create namespace sonataflow-infra

# PostgreSQL installation
kubectl create secret generic sonataflow-psql-postgresql --from-literal=postgres-username=postgres --from-literal=postgres-password=postgres -n sonataflow-infra
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install sonataflow-psql bitnami/postgresql -f ../e2e/resources/psql-values.yaml -n sonataflow-infra
sleep 60
kubectl wait --for=jsonpath='{.status.phase}'=Running "$(kubectl get pod -o name -n sonataflow-infra | grep sonataflow-psql-postgresql)" --timeout=300s -n sonataflow-infra

# Sonataflow operator
# kubectl create namespace sonataflow-operator-system
kubectl create -f https://raw.githubusercontent.com/kiegroup/kogito-serverless-operator/main/operator.yaml
kubectl apply -f ../e2e/resources/sonata-flow-operator.yaml
kubectl wait --for=jsonpath='{.status.phase}'=Running "$(kubectl get pod -o name -n sonataflow-operator-system | grep sonataflow-operator-controller-manager)" -n sonataflow-operator-system  --timeout=300s

# Core services of Sonataflow
kubectl apply -f ../e2e/resources/sonata-flow-platform.yaml
sleep 60
kubectl wait --for=jsonpath='{.status.phase}'=Running "$(kubectl get pod -o name -n sonataflow-infra | grep sonataflow-platform-jobs-service)" -n sonataflow-infra --timeout=300s
kubectl wait --for=jsonpath='{.status.phase}'=Running "$(kubectl get pod -o name -n sonataflow-infra | grep sonataflow-platform-data-index-service)" -n sonataflow-infra --timeout=300s