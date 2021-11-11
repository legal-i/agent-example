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
	@docker build --build-arg GITHUB_USER --build-arg GITHUB_TOKEN -f docker/Dockerfile -t "$(DOCKER_TAG)" .

## run: run docker image 
.PHONY: run
run: dockerize
	@docker run -e LEGALI_API_URL -e LEGALI_CLIENT_SECRET -t "$(DOCKER_TAG)"

# INTERNAL: pdfs for stresstest
S3_PREFIX=s3://legali-artifacts/workbench-data/
push-pdfs:
	@aws-vault exec legali-prod-admin -- aws s3 sync ./pdfs $(S3_PREFIX)agent-test

pull-pdfs:
	@aws-vault exec legali-prod -- aws s3 sync $(S3_PREFIX)agent-test ./pdfs --delete

.PHONY: setup 
