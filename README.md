# legal-i Agent

## Quickstart

1. Set your legal-i cloud credentials in `quickstart/agent.env`
2. In the `quickstart`-directory, run `docker compose build`.
3. Run `docker compose up agent`
4. The agent runs and starts transmitting data
5. In case you need monitoring or proxy support, you can start the corresponding services
    1. Monitoring: `docker compose up prometheus grafana`
        1. Open Grafana at http://localhost:3000/ (admin/admin)
        2. Add Prometheus datasource pointing to http://prometheus:9090/
    2. Http Proxy: `docker compose up squid`
        1. Adapt `agent.env` to use the proxy

### Monitoring

```
# Liveness (agent is up)
http://localhost:8085/actuator/health/liveness 

# Readiness (agent is can connect to the legal-i cloud)
http://localhost:8085/actuator/health/readiness

# Prometheus metrics
http://localhost:8085/actuator/prometheus

```

## Development

Make sure to set the secrets correctly via environment variables or a properties file:

```
LEGALI_API_URL=<>
LEGALI_CLIENT_ID=<>
LEGALI_CLIENT_SECRET=<>
```

In the Example Connector, you can see how the different APIs are called. JavaDoc can be downloaded for the SDK module for futher explanation and thrown exceptions.


### Overview:

- After the Spring Boot application is initialized, the agent tries to connect to the legal-i endpoints.
- If the connection can be established, an `HealthService.StartConnectorEvent` Event is published.
- This has the following effects:
    - Upon receiving this event, the `ExampleService` runs `ExampleThreads`. Those Example threads call some of the
      APIs.
        - The amount of threads and the runs per thread can be configured
        - Further, a path can be specified for choosing PDF files
    - The `ExampleRemoteEventService` starts listening to RemoteEvents that are triggered on the API.
        - As an example, he requests a `pong`-Event from the API.
        - This pong will be sent by the API asynchronously and be visible in the EventHandler

## Build, create docker image and run

See Makefile as a reference:

```
make ...
  lint        run verify and skip tests
  verify      run verify with tests
  build       build the agent
  dockerize   create agent docker image tagged legali-agent
  run         run docker image 
```

## Configuration and deployment

All configuration can be set as environment variables by Spring Boot configuration convention.

````
# Example Connector Config
legali.example.iterations=1
spring.task.execution.pool.max-size=1
spring.task.execution.pool.core-size=1

# Disable processing pipeline for development (do not use in production)
legali.default-metadata.legali.pipeline.disabled=true

# Endpoint and credentials for legal-i Cloud
legali.api-url=<>
legali.client-id=<>
legali.client-secret=<>

# Upload via cloudfront or proxied via api
legali.cloud-front-upload=true

# HTTP connection
#legali.request-connection-timeout-seconds=30
#legali.max-connection-retries=5
#legali.request-read-timeout-seconds=30
#legali.max-failed-heartbeats=5

# Proxy setup
#legali.http-proxy-host=localhost
#legali.http-proxy-port=3128

# Logging and Debugging
logging.level.root=INFO
logging.level.ch.legali.agent.example=INFO
logging.level.ch.legali.agent.sdk=INFO
logging.level.ch.legali.agent.sdk.internal=WARN

# Debug HTTP connection
#logging.level.feign = DEBUG
#legali.feign-log-level=FULL

# Monitoring
server.port=8085
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
management.endpoint.health.group.readiness.include=readinessState,agent
````

