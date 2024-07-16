# Requirements
This container images extends a basic UBI 9 image with the required tools:
* `git`
* `tar`
* `kustomize`
* `jq`
* `yq`
* `kn`
* `kubectl`

# Build and publish the image
Customize the `push` command to publish in your own repository:
```bash
 docker build -t quay.io/orchestrator/ubi9-pipeline:latest .
 docker push quay.io/orchestrator/ubi9-pipeline:latest
```
