# legal-i Agent

## Quickstart

1. Set legal-i cloud credentials in `example/agent.env`
2. In `./example`, run `docker-compose build`. NOTE: DO NOT run `docker compose build`, it won't pickup the env vars correctly!
3. Run `docker compose up`, the agent runs and starts transmitting data

Monitoring
- Grafana at http://localhost:3000/ (admin/admin)
- Add Prometheus datasource on http://prometheus:9090/
- TODO(mba): maybe setup an example dashboard

Endpoints
```
# Liveness (agent is up)
http://localhost:8085/actuator/health/liveness 

# Readiness (agent is can connect to the legal-i cloud)
http://localhost:8085/actuator/health/readiness

# Prometheus metrics
http://localhost:8085/actuator/prometheus

TODO(mba): do we have any custom 
```


## Development
Make sure to set the secrets correctly:

```
LEGALI_CLIENT_NAME=customer-dev
LEGALI_CLIENT_ID=<>
LEGALI_CLIENT_SECRET=<>
```

# Build docker image

## Deployment

If the connection has to go through a HTTP proxy specify the following environment variables
```
http.proxyHost=PROXY_HOST
http.proxyPort=PROXY_PORT
http.proxyUser=USERNAME
http.proxyPassword=PASSWORD
```