# FROM registry.redhat.io/openshift-serverless-1-tech-preview/logic-swf-builder-rhel8@sha256:d19b3ecaeac10e6aa03530008d25c8171254d561dc5519b9efd18dd4f0de5675 AS builder
# Using the builder image below to address bugs https://issues.redhat.com/browse/FLPATH-1141 and https://issues.redhat.com/browse/FLPATH-1127

ARG BUILDER_IMAGE

# The default builder image is the released OSL 1.33 https://catalog.redhat.com/software/containers/openshift-serverless-1/logic-swf-builder-rhel8/6614edd826a5be569c111884?container-tabs=gti
FROM ${BUILDER_IMAGE:-registry.redhat.io/openshift-serverless-1/logic-swf-builder-rhel8@sha256:9a4093e195bec163c609381e3c0ceeec55f6e229c6a1def6f362517886faea71} AS builder

#ENV MAVEN_REPO_URL=https://maven.repository.redhat.com/earlyaccess/all

# variables that can be overridden by the builder
# To add a Quarkus extension to your application
# When using nightly:
# ARG QUARKUS_EXTENSIONS=org.kie:kogito-addons-quarkus-jobs-knative-eventing:999-SNAPSHOT,org.kie:kie-addons-quarkus-persistence-jdbc:999-SNAPSHOT,io.quarkus:quarkus-jdbc-postgresql:3.8.4,io.quarkus:quarkus-agroal:3.8.4,org.kie:kie-addons-quarkus-monitoring-prometheus:999-SNAPSHOT,org.kie:kie-addons-quarkus-monitoring-sonataflow:999-SNAPSHOT
# When using prod:
ARG QUARKUS_EXTENSIONS=org.kie:kogito-addons-quarkus-jobs-knative-eventing:9.101.0.redhat-00007,org.kie:kie-addons-quarkus-persistence-jdbc:9.101.0.redhat-00007,io.quarkus:quarkus-jdbc-postgresql:3.8.4.redhat-00002,io.quarkus:quarkus-agroal:3.8.4.redhat-00002

# Args to pass to the Quarkus CLI
# add extension command
# ARG QUARKUS_ADD_EXTENSION_ARGS

# Additional java/mvn arguments to pass to the builder.
# This are is conventient to pass sonataflow and quarkus build time properties.
# Note that the maxYamlCodePoints parameter contols the maximum input size for 
#   YAML input files, and is currently set to 35000000 characters (~33MB in UTF-8).  
ARG MAVEN_ARGS_APPEND="-DmaxYamlCodePoints=35000000 -Dkogito.persistence.type=jdbc -Dquarkus.datasource.db-kind=postgresql -Dkogito.persistence.proto.marshaller=false"

# Argument for passing the resources folder if not current context dir
ARG WF_RESOURCES

# Copy from build context to skeleton resources project
COPY --chown=1001 ${WF_RESOURCES} ./resources/
RUN ls -la ./resources

ENV swf_home_dir=/home/kogito/serverless-workflow-project
RUN if [[ -d "./resources/src" ]]; then cp -r ./resources/src/* ./src/; fi
# Workaround for https://github.com/apache/incubator-kie-kogito-runtimes/issues/3725
RUN mvn quarkus:remove-extension -Dextension=org.kie:kie-addons-quarkus-monitoring-sonataflow:9.101.0.redhat-00007
RUN mvn quarkus:remove-extension -Dextension=org.kie:kie-addons-quarkus-monitoring-prometheus:9.101.0.redhat-00007

RUN /home/kogito/launch/build-app.sh ./resources

#=============================
# Runtime Run
#=============================
FROM registry.access.redhat.com/ubi9/openjdk-17:1.21-2


ARG FLOW_NAME
ARG FLOW_SUMMARY
ARG FLOW_DESCRIPTION

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used

COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/quarkus/ /deployments/quarkus/
COPY LICENSE /licenses/

EXPOSE 8080
USER 185
ENV AB_JOLOKIA_OFF=""
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

LABEL name="${FLOW_NAME}"
LABEL summary="${FLOW_SUMMARY}"
LABEL description="${FLOW_DESCRIPTION}"
LABEL io.k8s.description="${FLOW_DESCRIPTION}"
LABEL io.k8s.display-name="${FLOW_NAME}"
LABEL com.redhat.component="${FLOW_NAME}"
LABEL io.openshift.tags=""
