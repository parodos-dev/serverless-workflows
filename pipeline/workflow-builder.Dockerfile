FROM quay.io/kiegroup/kogito-swf-builder-nightly:main-2024-02-16 AS builder

# variables that can be overridden by the builder
# To add a Quarkus extension to your application
ARG QUARKUS_EXTENSIONS
# Args to pass to the Quarkus CLI
# add extension command
ARG QUARKUS_ADD_EXTENSION_ARGS

# Argument for passing the resources folder if not current context dir
ARG WF_RESOURCES

# Copy from build context to skeleton resources project
COPY --chown=1001 ${WF_RESOURCES} ./resources/
RUN ls -la ./resources

ENV swf_home_dir=/home/kogito/serverless-workflow-project
RUN if [[ -d "./resources/src" ]]; then cp -r ./resources/src/* ./src/; fi
RUN /home/kogito/launch/build-app.sh ./resources

#=============================
# Runtime Run
#=============================
FROM registry.access.redhat.com/ubi8/openjdk-17:latest

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used

COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=185 /home/kogito/serverless-workflow-project/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV AB_JOLOKIA_OFF=""
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
