# legal-i Agent

## Quickstart

*Prerequisites*

- Recent docker and docker-compose version installed
- Internet-Access to `*.legal-i.ch`

1. Set your legal-i cloud credentials in `quickstart/agent.env`
2. In the `quickstart`-directory, run `docker-compose build`.
3. Run `docker-compose up agent`
4. The agent runs and starts transmitting data
5. In case you need monitoring or proxy support, you can start the corresponding services
	1. Monitoring: `docker-compose up prometheus grafana`
		1. Open Grafana at http://localhost:3000/ (admin/admin)
		2. Add Prometheus datasource pointing to http://prometheus:9090/
		3. Currently, there is no default dashboard defined.
	2. Http Proxy: `docker-compose up squid`
		1. Adapt `agent.env` to use the proxy

### Monitoring

```
# Liveness (agent is up)
http://localhost:8085/actuator/health/liveness

# Readiness (agent can connect to the legal-i cloud)
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

In the Example Connector, you can see how the different APIs are called. JavaDoc can be downloaded for the SDK module
for further explanation and thrown exceptions.

### Overview:

- After the Spring Boot application is initialized, the agent tries to connect to the legal-i endpoints.
- If the connection can be established, an `HealthService.StartConnectorEvent` Event is published.
- This has the following effects:
	- Upon receiving this event, the `ExampleService` runs `ExampleThreads`. Those Example threads call some of the
	available APIs.
		- The amount of threads and the runs per thread can be configured
		- Further, a path can be specified for choosing PDF files
	- The `ExampleRemoteEventService` starts listening to RemoteEvents that are triggered on the API.
		- As an example, he requests a `pong`-Event from the API.
		- This pong will be sent by the API asynchronously and be visible in the EventHandler

### Entity Metadata

`LegalCase` and `SourceFiles` Entities contain a metadata field to add integration-specific key-value store, with
type `string` / `string`. Defaults can be set in the application config or via environment variables. Currently, the
following keys are supported:

```
SourceFile
# set dossier, supported value, see below. defaults to unknown
legali.dossiertype       = accident

# set doc type, supported values see below
legali.doctype          = type_medical_report

# set the issue date
legali.issuedate        = 2020-01-01

# set the document title
legali.title            = Dokumenttitel

# if mutlile languages are available, suffix with 2 letter language key
legali.title_fr         = titre du document

# document language in two character ke, if empty its detected
legali.lang             = de

Technical
# Disables processing for this file (to test APIs and integration)
legali.pipeline.disabled = true

```

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

## Configuration and Deployment

All configuration can be set as environment variables by Spring Boot configuration convention.

````
# Example Connector Config, set iterations and threads
legali.example.iterations=1
spring.task.execution.pool.max-size=1
spring.task.execution.pool.core-size=1

# Disable processing pipeline for development (do not use in production)
legali.default-metadata.legali.pipeline.disabled=true

# Endpoint and credentials for legal-i cloud
legali.api-url=<>
legali.client-id=<>
legali.client-secret=<>

# Upload via cloudfront or proxied via api, use cloud front upload whenever possible
legali.cloud-front-upload=true

# HTTP connection
#legali.request-connection-timeout-seconds=30
#legali.max-connection-retries=5
#legali.request-read-timeout-seconds=90
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

### Internet Access

The agent needs to be allowed to access `*.legal-i.ch` on 443.

## References

## Dossier Types

```
unknown           : Anderes (default)
health            : Krankenversicherung
health_allowance  : Krankentaggeld (KVG und VVG)
liability         : Haftpflichtrecht
bvg               : BVG
vvg               : VVG
accident          : Unfall
suva              : Unfall (SUVA)
iv-ag             : IV Aargau
iv-ai             : IV Appenzell Innerrhoden
iv-ar             : IV Appenzell Ausserrhoden
iv-be             : IV Bern
iv-bl             : IV Basel-Landschaft
iv-bs             : IV Basel-Stadt
iv-fr             : IV Freiburg
iv-ge             : IV Genf
iv-gl             : IV Glarus
iv-gr             : IV Graubünden
iv-ju             : IV Jura
iv-lu             : IV Luzern
iv-ne             : IV Neuenburg
iv-nw             : IV Nidwalden
iv-ow             : IV Obwalden
iv-sg             : IV St. Gallen
iv-sh             : IV Schaffhausen
iv-so             : IV Solothurn
iv-sz             : IV Schwyz
iv-tg             : IV Thurgau
iv-ti             : IV Tessin
iv-ur             : IV Uri
iv-vd             : IV Waadt
iv-vs             : IV Wallis
iv-zg             : IV Zug
iv-zh             : IV Zürich
```

### Document Types

```
type_admin                    : Admin
type_admin_claim_report_uvg   : Schadenmeldung UVG
type_admin_facts_sheet        : Feststellungsblatt
type_admin_iv_registration    : IV Anmeldung
type_admin_table_of_content   : Inhaltsverzeichnis
type_financial                : Finanziell
type_financial_allowance_overview: Detaillierte Taggeldübersicht
type_financial_ik_statement   : IK Auszug
type_financial_questionnaire  : Arbeitgeberfragebogen
type_legal                    : Rechtlich
type_legal_attorney_submission: Anwaltliche Eingaben
type_legal_court_decision     : Urteil
type_legal_disposal           : Verfügung
type_medical                  : Medizinisch
type_medical_certificate      : Arztzeugnis
type_medical_expert_opinion   : Gutachten
type_medical_form             : Formular
type_medical_report           : Arztbericht
```

## IAM Integration
Users are included with single sign-on. Roles and permissions are managed by group memberships.


### Enterprise IDP connections
See https://auth0.com/docs/connections/enterprise

### Roles and Authorization
Every user needs to have at least one valid legal-i role to access legal-i. The role is given to the user by assigning him a group with a specific pattern.

- **Tenant Admin**
- has group that contains `*legali_admin*`
- has access to...
	- all legal cases (without permission check)
	- admin functions and agent panel


- **Tech Admin**
	- has group that contains `*legali_tech*`
	- has access to...
		- admin and agent panel
	- has no access to...
		- legal cases and data


- **Basic**
	- has group that contains `*legali_basic*`
	- has access to...
		- cases that he has access (see permission groups)
	- has no access to...
		- admin and agent panel

### Permission groups
All other groups that contain `*legali*` are used as permission groups.
A basic user only has access to a legal case if they have at least one matching group.
