# legal-i Agent

## Component and Delivery

- The legal-i agent is a dockerized Spring Boot application that ensures solid two-way communication between the customers' environment and the legal-i cloud.
- The component has two sides: The SDK that talks to the cloud and the connector that speaks to the customer system.
- The latest version is available on legal-i's private GitHub repository.

## Quickstart

*Prerequisites*

- Access to legal-i's GitHub repository on https://github.com/legal-i/agent-example
- Agent credentials to access your environment on the legal-i cloud
- Recent docker and docker-compose versions installed
- Internet-Access to `*.legal-i.ch`. If whitelisting is not an option, refer to the subsection [Agent Authorization](#agent-authorization)
and [API Description](#api-description).

1. Set your legal-i cloud credentials in `quickstart/agent.env`
2. In the `quickstart`-directory, run `docker-compose build`.
3. Run `docker-compose up agent`
4. The agent runs and starts transmitting data
5. Check the agent status in the UI (menu in avatar).
6. In case you need monitoring or proxy support, you can start the corresponding services
	1. Monitoring: `docker-compose up prometheus grafana`
		1. Open Grafana at http://localhost:3000/ (admin/adminex)
		2. Add Prometheus data source pointing to http://prometheus:9090/
		3. Currently, there is no default dashboard defined.
	2. HTTP Proxy: `docker-compose up squid`
		1. Adapt `agent.env` to use the proxy

## Development

Make sure to set the secrets correctly via environment variables or a properties file:

```
LEGALI_API_URL=https://{prefix}-agents.legal-i.ch/api/v1
LEGALI_AUTH_URL=https://auth.legal-i.ch/
LEGALI_CLIENT_ID=<>
LEGALI_CLIENT_SECRET=<>
```

In the Example Connector, you see how to call the different APIs. JavaDoc can be downloaded for the SDK module
for further explanation and thrown exceptions.

### Overview:

- After the Spring Boot application is initialized, the agent tries to connect to the legal-i endpoints.
- If the connection can be established, an `HealthService.StartConnectorEvent` Event is published.
- This has the following effects:
	- Upon receiving this event, the `ExampleService` runs `ExampleThread`. The example threads call the
	available APIs.
		- The number of threads and the runs per thread can be configured
		- Further, a path can be specified for choosing PDF files
	- The `ExampleEventService` starts listening to events triggered on the API.
		- As an example, he requests a `pong`-Event from the API.
		- This pong will be sent by the API asynchronously and be visible in the EventHandler
- SDK entities and methods contain JavaDoc annotations.

### Monitoring
The following endpoints are provided in the Example Agent.
```
# Liveness (agent is up)
http://localhost:8085/actuator/health/liveness

# Readiness (agent can connect to the legal-i cloud)
http://localhost:8085/actuator/health/readiness

# Prometheus metrics
http://localhost:8085/actuator/prometheus

```

### Agent Authorization
- The legal-i SDK authorizes on the legal-i IDP `https://auth.legal-i.ch` using the OAuth 2.0 Client Credentials Grant.
- The client credentials can be rotated by tenant admins using the legal-i frontend.
- `https://auth.legal-i.ch/` is independent of the customer's workspacess.

The following outbound endpoints need to be accessible for authenticating the SDK.
```
POST https://auth.legal-i.ch/oauth/token
```

### API Description
- Agents require outbound access to the environment-specific agent endpoints, normally `https://{prefix}-agents.legal-i.ch`.
- For a description of the legal-i API and the OpenAPI3 definition, refer to `https://agents.legal-i.ch/api/v1/doc/swagger.html`

### File Transfer
The SDK transfers files directly from and to AWS using CLOUDFRONT using pre-signed URLs. Therefore, the following extra
endpoints must be accessible outbound:
```
GET https://api.legal-i.ch/api/v1/store/{any}/{any}/{any} # NOTE: here it needs to be https://api.legal-i.ch

# The files are transfered over these endpoints:
PUT https://static-temp.legal-i.ch/{any}/{any}`
GET https://static-export.legal-i.ch/{any}/{any}
GET https://static-files.legal-i.ch/{any}/{any}
```

- The file transfer API is subject to change.
- The `LEGALI` file service is deprecated and only used for development.
```
legali.fileservice = CLOUDFRONT
```

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

# if multiple languages are available, suffix with a two-letter language key
legali.title_fr         = titre du document

# document language in two-character key. If empty, it's detected
legali.lang             = de

Technical
# Disables processing for this file (to test APIs and integration)
legali.pipeline.disabled = true

```

### Events

The agent must subscribe to the events it wants to receive.

```
@PostConstruct
public void init() {
	this.eventService.subscribe(PongEvent.class, ExportPublishedEvent.class);
}
```

After subscribing, the agent can listen for events by creating event listener methods.
Annotate methods with the `@EventListener` annotation and specify the event class as the method parameter.

```
@EventListener
public void handle(PongEvent event) {
	log.info("🏓 PingPong Event received: " + "\nid " + event.getUuid());
	this.eventService.acknowledge(event)
}
```

Every event handler must ack the event. When the API does not receive an ack, it will send the event
again after 5 minutes.

```
this.eventService.acknowledge(event)
```
*Implemented Events:*

`PongEvent`
Emitted after a PongRequest is requested for debugging.
Request with `this.eventClient.ping();`

`LegalCaseCreatedEvent`
Emitted when a user creates a new legal case via the frontend.

`LegalCaseUpdatedEvent`
Emitted when a user updates a legal case via the frontend.

`LegalCaseStatusChangedEvent`
Emitted when a user changes the status of a legal case via the frontend (OPEN, ARCHIVED).

`LegalCaseReadyEvent`
Emitted when all sourcefiles of a legal case are successfully processed.

`SourceFileCreatedEvent`.
Emitted when a user creates a source file via the frontend.

`SourceFileUpdatedEvent`
Emitted when a user changes the Dossier Type (aka Field / Akte) in the frontend

`SourceFileTaskFailedEvent`
Emitted by the pipeline when processing of the source file failed.

`ExportCreatedEvent`
Emitted when a user creates a new export through the frontend.

`ExportPublishedEvent`
Emitted when a user publishes a new export with a link or an email.

`ExportViewedEvent`
Emitted when an external user opens/downloads an exported pdf.

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

All configurations can be set as environment variables by Spring Boot configuration convention.

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

# Upload via CloudFront or proxied via API, use cloud front upload whenever possible
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
logging.level.ch.legali.sdk.example=INFO
logging.level.ch.legali.sdk.sdk=INFO
logging.level.ch.legali.sdk.sdk.internal=WARN

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


### Dossier Types

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
army              : Militärversicherung
```

### Document Types

```
type_admin    Admin
type_admin_claim_report_uvg         : Schaden- / Krankheitsmeldungen
type_admin_facts_sheet              : Feststellungsblätter
type_admin_iv_registration          : IV Anmeldungen
type_admin_protocol                 : Protokolle
type_admin_table_of_content         : Inhaltsverzeichnisse
type_correspondence                 : Korrespondenz
type_correspondence_external_emails : Externe Emails
type_correspondence_internal_emails : Interne Emails
type_correspondence_letters         : Briefe
type_financial                      : Finanziell
type_financial_allowance_overview   : Taggeld-Abrechnungen
type_financial_invoice              : Rechnungen
type_internal                       : Interne-Dokumente
type_internal_antifraud             : Akten der internen Betrugsbekämpfungsstelle
type_internal_reports               : Interne Berichte
type_legal                          : Rechtlich
type_legal_attorney_submission      : Anwaltliche Eingaben
type_legal_court_decision           : Urteile
type_legal_criminal_file            : Strafakten
type_legal_disposal                 : Verfügungen
type_legal_objection                : Einsprachen
type_legal_objection_decision       : Einsprache-Entscheide
type_legal_pre_disposal             : Vorbescheide / Formlose Ablehnungen
type_legal_proxy                    : Vollmachten
type_legal_submissions_court        : Eingaben Gerichtsverfahren
type_medical                        : Medizinisch
type_medical_certificate            : AUF-Zeugnisse
type_medical_cost_credit            : Kostengutsprachen
type_medical_expert_opinion         : Gutachten
type_medical_insurance_report       : Vers. interne Arztberichte
type_medical_prescription           : Med. Verordnungen
type_medical_report                 : Arztberichte
type_other                          : Andere
type_other_phone_memo               : Telefon- / Aktennotizen
type_profession                     : Berufliches
type_profession_cv                  : Lebensläufe
type_profession_employment_contract : Arbeitsverträge
type_profession_ik_statement        : IK-Auszüge
type_profession_questionnaire       : Arbeitgeberfragebogen
type_profession_wage_statements     : Lohnabrechnungen
```