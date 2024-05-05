FROM registry.access.redhat.com/ubi9-minimal

# Install Git
RUN microdnf install -y git tar jq && \
    microdnf clean all

RUN mkdir -p /usr/local/bin && \
    curl -sSLo /usr/local/bin/yq https://github.com/mikefarah/yq/releases/download/v4.43.1/yq_linux_amd64 && \
    chmod +x /usr/local/bin/yq

RUN curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
RUN mv kustomize /usr/local/bin

