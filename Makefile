DOCKER_TAG=legali-agent

ifeq ($(OS),Windows_NT) 
    maven_cmd := mvnw.cmd
else
    maven_cmd := ./mvnw
endif

## help: print this help message
.PHONY: help
help:
	@echo 'Usage:'
	@sed -n 's/^##//p' ${MAKEFILE_LIST} | column -t -s ':' |  sed -e 's/^/ /'

## format: run spotless google java formatter
.PHONY: format
format:
	@${maven_cmd} spotless:apply

## lint: run verify and skip tests
.PHONY: lint
lint:
	@${maven_cmd} -Dmaven.test.skip=true verify

## verify: run verify with tests
.PHONY: verify
verify:
	@${maven_cmd} verify

## build: build the agent
.PHONY: build
build:
	@${maven_cmd} package -DskipTests

## dockerize: create agent docker image 
.PHONY: dockerize
dockerize:
	@docker build -f docker/Dockerfile -t "$(DOCKER_TAG)" .

## run: run docker image 
.PHONY: run
run: dockerize
	@docker run -e LEGALI_API_URL -e LEGALI_CLIENT_SECRET -t "$(DOCKER_TAG)"
