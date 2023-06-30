SHELL:=/bin/bash

DOCKER_TAG=legali-agent-example-spring

ifeq ($(OS),Windows_NT)
    maven_cmd := .\mvnw.cmd
else
    maven_cmd := ./mvnw
endif

## help: print this help message
help:
	@echo 'Usage:'
	@sed -n 's/^##//p' ${MAKEFILE_LIST} | column -t -s ':' |  sed -e 's/^/ /'

## format: run spotless google java formatter
format:
	@${maven_cmd} spotless:apply

## lint: run verify and skip tests
lint:
	@${maven_cmd} -Dmaven.test.skip=true verify

## show dependency and plugin updates
show-updates:
	@${maven_cmd} versions:display-dependency-updates
	@${maven_cmd} versions:display-plugin-updates

## verify: run verify with tests
verify:
	@${maven_cmd} verify

## build: build the agent
build:
	@${maven_cmd} package -DskipTests

## dockerize: create agent docker image
dockerize:
	@docker build -f docker/Dockerfile -t "$(DOCKER_TAG)" .

## run: run docker image
run: dockerize
	@docker run -e LEGALI_API_URL -e LEGALI_CLIENT_SECRET -t "$(DOCKER_TAG)"
