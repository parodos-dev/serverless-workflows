#!/bin/bash

CONTAINER_ENGINE=$1
WORKDIR=$2
WORKFLOW_ID=$3
APPLICATION_ID=$4
JDK_IMAGE=$5
MVN_OPTS=$6

cd "${WORKDIR}"/"${WORKFLOW_ID}"/"${APPLICATION_ID}" || exit
${CONTAINER_ENGINE} run --rm -v "${WORKDIR}":/workdir -e MVN_OPTS="${MVN_OPTS}" -w /workdir/"${WORKFLOW_ID}"/"${APPLICATION_ID}" \
  --user root "${JDK_IMAGE}" mvn "${MVN_OPTS}" clean package -DskipTests

