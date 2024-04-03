VERSION="2.0.0"

download-openapi:
	curl https://raw.githubusercontent.com/konveyor/move2kube-api/main/assets/openapi.json -o openapi.json

generate: download-openapi
	rm -rf java-client
	docker run --rm \
		-u $(id -u):$(id -g) \
		-v $(PWD):/tmp \
		-e GENERATE_PERMISSIONS=true \
		openapitools/openapi-generator-cli generate \
		-i /tmp/openapi.json \
		-g java \
		-o /tmp/java-client \
		--invoker-package dev.parodos.move2kube \
		--model-package dev.parodos.move2kube.client.model \
		--api-package dev.parodos.move2kube.api \
		--group-id dev.parodos \
		--artifact-id move2kube \
		--artifact-version v$(VERSION) \
		--library apache-httpclient

install: download-openapi generate
	rm -rf $(PWD)/java-client/src/test
	cd java-client && mvn clean install
