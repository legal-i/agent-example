# legal-i Agent

## Development

Add the following dependency to your pom.xml

```
    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub Maven Repository</name>
            <url>https://maven.pkg.github.com/legal-i/agent-sdk</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>ch.legali</groupId>
            <artifactId>agent-sdk</artifactId>
            <version>0.0.1</version>
        </dependency>
    </dependencies>
```

To download the artifact you need a GitHub token in your settings.xml file

```
  <servers>
    <server>
      <id>github</id>
      <username>GITHUB_USER_NAME</username>
      <password>PERSONAL_GITHUB_ACCESSTOKEN_TOKEN</password>
    </server>
  </servers>
```  


Make sure to set the secret correctly:

```
LEGALI_CLIENT_SECRET=<>
```

## Connector Livecycle

Connectors can be started on the `HealthService.StartConnectorEvent`.

## Deployment

If the connection has to go through a HTTP proxy specify the following environment variables
```
http.proxyHost=PROXY_HOST
http.proxyPort=PROXY_PORT
http.proxyUser=USERNAME
http.proxyPassword=PASSWORD
```

### Liveness and Readiness

The spring boot actuator indicate if the agent is alive and is ready to accept data. The agent is ready, if the
connection to the legal-i cloud is established.

```
http://localhost:8085/actuator/health/liveness
http://localhost:8085/actuator/health/readiness
```


### Prometheus

Endpoint:
```
http://localhost:8085/actuator/prometheus
```

