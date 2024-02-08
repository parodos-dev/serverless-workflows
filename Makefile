# Requirements
# docker
# curl
# git

# CONTAINER_ENGINE ?= $(shell which podman >/dev/null 2>&1 && echo podman || echo docker)
CONTAINER_ENGINE := docker
ifneq (,$(wildcard $(CURDIR)/.docker))
	DOCKER_CONF := $(CURDIR)/.docker
else
	DOCKER_CONF := $(HOME)/.docker
endif

WORKDIR := $(shell pwd)
ifeq ($(shell uname),Darwin)
    WORKDIR := /tmp/serverless-workflows
endif

# Override for PR creation
GIT_USER_NAME ?= $(shell git config --get user.name)
GIT_USER_EMAIL ?= $(shell git config --get user.email)

# LINUX_IMAGE ?= ubuntu:latest
# LINUX_IMAGE ?= registry.access.redhat.com/ubi9-minimal
LINUX_IMAGE ?= quay.io/orchestrator/ubi9-pipeline:latest

WORKFLOW_ID := ""
IMAGE_NAME := ""
REGISTRY ?= quay.io
REGISTRY_REPO ?= orchestrator
IMAGE_PREFIX ?= serverless-workflow
IMAGE_TAG = $(shell git rev-parse --short=8 HEAD)
DOCKERFILE = pipeline/workflow-builder.Dockerfile

# TODO use parodos-dev
WF_HELM_REPO := https://github.com/dmartinol/serverless-workflows-helm

.PHONY: all

all: escalation

## Build:
build: BUILD_ARGS=--build-arg WF_RESOURCES=$(WORKFLOW_ID) \
	--build-arg QUARKUS_EXTENSIONS=org.kie.kogito:kogito-addons-quarkus-jobs-knative-eventing:999-SNAPSHOT
build: EXTRA_ARGS=--ulimit nofile=4096:4096
build:
	@echo "Building $(IMAGE_NAME)"
	@$(CONTAINER_ENGINE) build -f $(DOCKERFILE) \
		$(BUILD_ARGS) $(EXTRA_ARGS) \
		--tag ${IMAGE_NAME}:${IMAGE_TAG} --tag ${IMAGE_NAME}:latest .

push:
	@echo "Pushing $(IMAGE_NAME)"
	# @$(CONTAINER_ENGINE) --config=${DOCKER_CONF} push ${IMAGE_NAME}:latest
	# @$(CONTAINER_ENGINE) --config=${DOCKER_CONF} push ${IMAGE_NAME}:${IMAGE_TAG}
	@$(CONTAINER_ENGINE) push ${IMAGE_NAME}:latest
	@$(CONTAINER_ENGINE) push ${IMAGE_NAME}:${IMAGE_TAG}

save-oci: IMAGE_NAME=$(REGISTRY)/$(REGISTRY_REPO)/$(IMAGE_PREFIX)-$(WORKFLOW_ID)
save-oci: OCI_NAME=$(IMAGE_PREFIX)-$(WORKFLOW_ID)-$(IMAGE_TAG).tar
save-oci:
	@echo "Saving OCI archive $(OCI_NAME)"
	@$(CONTAINER_ENGINE) save ${IMAGE_NAME}:${IMAGE_TAG} -o ${OCI_NAME}

prepare-workdir:
ifeq ($(shell uname),Darwin)
	echo "Preparing workdir $(WORKDIR)"
	rm -rf $(WORKDIR)
	mkdir -p $(WORKDIR)
	cp -R . $(WORKDIR)
	cd $(WORKDIR)
else
	echo "Workdir is $(WORKDIR)"
endif

gen-manifests: prepare-workdir
	cd $(WORKDIR)
	@$(CONTAINER_ENGINE) run --rm -v $(WORKDIR):/workdir -w /workdir \
		$(LINUX_IMAGE) /bin/bash -c "./gen_manifests.sh $(WORKFLOW_ID)"

prepare-commit:
	cd $(WORKDIR)
	@$(CONTAINER_ENGINE) run --rm -v $(WORKDIR):/workdir -w /workdir \
		$(LINUX_IMAGE) /bin/bash -c "./prepare_commit.sh '$(GIT_USER_NAME)' $(GIT_USER_EMAIL) $(WF_HELM_REPO) $(WORKFLOW_ID) $(IMAGE_NAME) $(IMAGE_TAG)"

# Target: escalation
build-and-push-escalation: WORKFLOW_ID=escalation
build-and-push-escalation: IMAGE_NAME=$(REGISTRY)/$(REGISTRY_REPO)/$(IMAGE_PREFIX)-$(WORKFLOW_ID)
build-and-push-escalation: build push save-oci

escalation-gen-manifests: WORKFLOW_ID=escalation
escalation-gen-manifests: IMAGE_NAME=$(REGISTRY)/$(REGISTRY_REPO)/$(IMAGE_PREFIX)-$(WORKFLOW_ID)
escalation-gen-manifests: gen-manifests prepare-commit

escalation: WORKFLOW_ID=escalation
escalation: build-and-push-escalation escalation-gen-manifests prepare-commit