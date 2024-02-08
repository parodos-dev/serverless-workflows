FROM registry.access.redhat.com/ubi9-minimal

# Install Git
RUN microdnf install -y git tar && \
    microdnf clean all

RUN curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
RUN mv kustomize /usr/local/bin

