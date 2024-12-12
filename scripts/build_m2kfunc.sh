#!/bin/bash -x

CONTAINER_ENGINE=$1
WORKDIR=$2
WORKFLOW_ID=$3
APPLICATION_ID=$4
JDK_IMAGE=$5
MVN_OPTS=$6

VERSION="2.0.0"

cd "${WORKDIR}"/workflows/"${WORKFLOW_ID}"/"${APPLICATION_ID}"/move2kubeAPI || exit
curl https://raw.githubusercontent.com/konveyor/move2kube-api/main/assets/openapi.json -o openapi.json

rm -rf java-client
${CONTAINER_ENGINE} run --rm -v "${PWD}":/tmp -e GENERATE_PERMISSIONS=true openapitools/openapi-generator-cli \
  generate -i /tmp/openapi.json -g java -o /tmp/java-client \
  --invoker-package io.rhdhorchestrator.move2kube \
  --model-package io.rhdhorchestrator.move2kube.client.model \
  --api-package io.rhdhorchestrator.move2kube.api \
  --group-id io.rhdhorchestrator --artifact-id move2kube --artifact-version v${VERSION} \
  --library apache-httpclient

${CONTAINER_ENGINE} run --rm -v "${WORKDIR}":/workdir -e MVN_OPTS="${MVN_OPTS}" -w /workdir/workflows/"${WORKFLOW_ID}" \
  "${JDK_IMAGE}" mvn "${MVN_OPTS}" -q clean install -DskipTests