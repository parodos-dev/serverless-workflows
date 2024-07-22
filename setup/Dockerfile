FROM registry.access.redhat.com/ubi9-minimal

# Install Git
RUN microdnf install -y which git tar jq && \
    microdnf clean all

RUN mkdir -p /usr/local/bin && \
    curl -sSLo /usr/local/bin/yq https://github.com/mikefarah/yq/releases/download/v4.43.1/yq_linux_amd64 && \
    chmod +x /usr/local/bin/yq

RUN curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
RUN mv kustomize /usr/local/bin

RUN curl -sSL "https://mirror.openshift.com/pub/openshift-v4/clients/serverless/1.11.2/kn-linux-amd64.tar.gz" \
    | tar -xz --no-same-owner && chmod +x kn-linux-amd64 && mv kn-linux-amd64 /usr/local/bin/kn

RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
    && chmod +x kubectl && mv kubectl /usr/local/bin/

