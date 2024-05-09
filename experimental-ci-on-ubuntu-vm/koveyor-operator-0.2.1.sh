#!/bin/bash

set -x
set -e

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
sleep 30
kubectl wait --for=condition=Ready=true pods -l "app.kubernetes.io/name=tackle-ui" -n my-konveyor-operator --timeout=240s
