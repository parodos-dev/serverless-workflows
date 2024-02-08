# Requirements
# docker or podman installed and running
# git installed
# .git_token file exists with current Token (or injected as the GIT_TOKEN env var)
# Linux OS

ifndef WORKFLOW_ID
$(error WORKFLOW_ID variable is not defined. Please provide the required value)
endif

CONTAINER_ENGINE ?= $(shell which podman >/dev/null 2>&1 && echo podman || echo docker)
ifneq (,$(wildcard $(CURDIR)/.docker))
	DOCKER_CONF := $(CURDIR)/.docker
else
	DOCKER_CONF := $(HOME)/.docker
endif

WORKDIR ?= $(shell mktemp -d)
ifeq ($(shell uname),Darwin)
	# Use a fixed folder to simplify limactl configuration (must be mounted with Write permissions)
	WORKDIR := /tmp/serverless-workflows
endif
SCRIPTS_DIR := scripts

GIT_USER_NAME ?= $(shell git config --get user.name)
GIT_USER_EMAIL ?= $(shell git config --get user.email)
GIT_REMOTE_URL := $(shell git config --get remote.origin.url)
GIT_REMOTE_URL := $(shell echo "$(GIT_REMOTE_URL)" | sed 's/\.git//')
GIT_REMOTE_URL := $(shell echo "$(GIT_REMOTE_URL)" | sed 's/git@/https:\/\//')
GIT_REMOTE_URL := $(shell echo "$(GIT_REMOTE_URL)" | sed 's/com:/com\//')
PR_OR_COMMIT_URL ?= "$(GIT_REMOTE_URL)/commits/$(shell git rev-parse --short=8 HEAD)"
ifndef GIT_TOKEN
ifeq ($(wildcard .git_token),)
$(error No Git token found. Please provide it either via the GIT_TOKEN variable or the .git_token file)
endif
GIT_TOKEN ?= $(shell cat .git_token)
endif
LINUX_IMAGE ?= quay.io/orchestrator/ubi9-pipeline:latest

IMAGE_NAME := ""
REGISTRY ?= quay.io
REGISTRY_REPO ?= $(shell id -un)
REGISTRY_USERNAME ?= ""
REGISTRY_PASSWORD ?= ""
IMAGE_PREFIX ?= serverless-workflow
IMAGE_TAG ?= $(shell git rev-parse --short=8 HEAD)
DOCKERFILE = pipeline/workflow-builder.Dockerfile

IMAGE_NAME = $(REGISTRY)/$(REGISTRY_REPO)/$(IMAGE_PREFIX)-$(WORKFLOW_ID)

DEPLOYMENT_REPO ?= parodos-dev/serverless-workflows-helm

.PHONY: all

all: build-image push-image gen-manifests push-manifests

# Target: prepare-workdir
# Description: copies the local repo content in a temporary WORKDIR for file manipulation.
# Usage: make prepare-workdir
prepare-workdir:
	@echo "Preparing workdir $(WORKDIR)"
	@rm -rf $(WORKDIR)
	@mkdir -p $(WORKDIR)
	@cp -R . $(WORKDIR)

# Target: build-image
# Description: Builds the workflow containerized image from the given WORKDIR.
# Depend on: prepare-workdir target.
# Usage: make build-image
build-image: BUILD_ARGS=--build-arg WF_RESOURCES=$(WORKFLOW_ID)
build-image: EXTRA_ARGS=--ulimit nofile=4096:4096
build-image: prepare-workdir
	@echo "Building $(IMAGE_NAME)"
	@$(CONTAINER_ENGINE) build -f $(DOCKERFILE) \
		$(BUILD_ARGS) $(EXTRA_ARGS) \
		--tag ${IMAGE_NAME}:${IMAGE_TAG} --tag ${IMAGE_NAME}:latest .

# Target: push-image
# Description: Pushes the workflow containerized image to the configured REGISTRY.
# Usage: make push-image
push-image: 
	@echo "Pushing $(IMAGE_NAME)"
ifneq ($(strip $(REGISTRY_USERNAME)),"")
ifneq ($(strip $(REGISTRY_PASSWORD)),"")
	@echo "${REGISTRY_PASSWORD}" | $(CONTAINER_ENGINE) login -u ${REGISTRY_USERNAME} --password-stdin ${REGISTRY}
endif
endif
	@$(CONTAINER_ENGINE) push ${IMAGE_NAME}:latest
	@$(CONTAINER_ENGINE) push ${IMAGE_NAME}:${IMAGE_TAG}

# Target: save-oci
# Description: Extracts the containerized image to a local file in the current folder.
# Depend on: build-image target.
# Usage: make save-oci
save-oci: OCI_NAME=$(IMAGE_PREFIX)-$(WORKFLOW_ID)-$(IMAGE_TAG).tar
save-oci: build-image
	@echo "Saving OCI archive $(OCI_NAME)"
	@$(CONTAINER_ENGINE) save ${IMAGE_NAME}:${IMAGE_TAG} -o ${OCI_NAME}


# Target: gen-manifests
# Description: Generates the k8s manifests for the WORKFLOW_ID workflow under the configured WORKDIR.
# Depend on: prepare-workdir target.
# Usage: make gen-manifests
gen-manifests: prepare-workdir
	cd $(WORKDIR)
	@$(CONTAINER_ENGINE) run --rm -v $(WORKDIR):/workdir -w /workdir \
		$(LINUX_IMAGE) /bin/bash -c "${SCRIPTS_DIR}/gen_manifests.sh $(WORKFLOW_ID)"

# Target: push-manifests
# Description: Pushes the generated k8s manifests from the configured WORKDIR to the 
# configured DEPLOYMENT_REPO.
# Usage: make push-manifests
push-manifests: prepare-workdir
	cd $(WORKDIR)
	@$(CONTAINER_ENGINE) run --rm -v $(WORKDIR):/workdir -w /workdir \
		$(LINUX_IMAGE) /bin/bash -c "${SCRIPTS_DIR}/push_manifests.sh '$(GIT_USER_NAME)' $(GIT_USER_EMAIL) $(GIT_TOKEN) $(PR_OR_COMMIT_URL) $(DEPLOYMENT_REPO) $(WORKFLOW_ID) $(IMAGE_NAME) $(IMAGE_TAG)"
