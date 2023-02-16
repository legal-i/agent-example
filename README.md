# legal-i Agent

## Overview
- The legal-i Example Agent is a Spring Boot application showcasing how to build a customer-specific agent.
- The legal-i SDK ensures solid two-way communication between the customers' environment and the legal-i cloud.
- The latest version is available on legal-i's GitHub repository.

&nbsp;
&nbsp;

---

&nbsp;
&nbsp;

## No-Code Quickstart

*Prerequisites*

- Access to legal-i's GitHub repository on https://github.com/legal-i/agent-example
- Agent credentials for your workspace, available through the app.
- Recent docker and docker-compose
- Internet-Access to `*.legal-i.ch`.

1. Setup legal-i cloud credentials in `quickstart/agent.env`
2. In the `quickstart`-directory, run `docker-compose build`.
3. Run `docker-compose up agent`
4. The agent runs and starts exchanging data
5. On the app, check the agent status in the Workspace Settings (menu in avatar).
6. In case you need monitoring or proxy support, you can start the corresponding services
	1. Monitoring: `docker-compose up prometheus grafana`
		1. Open Grafana at http://localhost:3000/ (admin/adminex)
		2. Add Prometheus data source pointing to http://prometheus:9090/
	2. HTTP Proxy: `docker-compose up squid`,  adapt `agent.env` to use the proxy

&nbsp;
&nbsp;

---

&nbsp;
&nbsp;

## Development

### Credentials
Make sure to set the secrets correctly via environment variables or a properties file:

```
LEGALI_API_URL=https://{customer-prefix}.agents.legal-i.ch/agents/v1
LEGALI_AUTH_URL=https://auth.legal-i.ch/
LEGALI_CLIENT_ID=<from workspace>
LEGALI_CLIENT_SECRET=<from workspace>
```

In the `ExampleThread.java` and `ExampleEventService.java`, you see examples of how to call different APIs and subscribe to events. JavaDoc can be downloaded for the SDK module for further explanation and thrown exceptions.

### Application Logic

- After the Spring Boot application is initialized, the agent connects to the legal-i cloud.
- If the connection can be established, an `HealthService.StartConnectorEvent` Event is published.
- This has the following effects:
	- Upon receiving this event, the `ExampleService` runs `ExampleThread` that creates legal cases and adds source files on the legal-i workspace.
	- The `ExampleEventService` starts listening to events triggered on the API.
		- As an example, he requests a `pong`-Event from the API.
		- This pong will be sent by the API asynchronously and be visible in the EventHandler
- All SDK entities and methods contain JavaDoc annotations.

### Build, run, and monitor
See Makefile for a reference of build targets:

```
make ...
lint        run verify and skip tests
verify      run verify with tests
build       build the agent
dockerize   create agent docker image tagged legali-agent
run         run docker image
```

The following endpoints are provided in the Example Agent.
```
# Liveness (agent is up)
http://localhost:8085/actuator/health/liveness

# Readiness (agent can connect to the legal-i cloud)
http://localhost:8085/actuator/health/readiness

# Prometheus metrics
http://localhost:8085/actuator/prometheus

```

&nbsp;
&nbsp;

---

&nbsp;
&nbsp;


## Agent Authorization
- The legal-i SDK is authorized via legal-i's IDP `https://auth.legal-i.ch` using the OAuth 2.0 Client Credentials Grant.
- Client credentials can be rotated by workspace admins in the legal-i app.

&nbsp;
## Protocols and Firewall
- The legal-i SDK communicates via HTTPS/TLS1.2 to REST endpoints
- Outbound access is required to
	- the environment-specific agent endpoints, i.e., `https://{customer-prefix}.agents.legal-i.ch`
		- For OpenAPI3 specs refer to `https://agents.legal-i.ch/doc/swagger.html`
	- legal-i's data buckets on AWS at `https://upload.legal-i.ch/*` and `https://data.legal-i.ch/*`
	- legal-i's IDP: `https://auth.legal-i.ch/oauth/token`
- No inbound access is required.

&nbsp;

### File Transfer API
The SDK transfers files directly from and to AWS using presigned URLs. Therefore, the following extra
endpoints must be accessible outbound:
```
# The files are transferred to these endpoints:
PUT https://upload.legal-i.ch/{any}
GET https://data.legal-i.ch/{any}
```
```
# Enabled
legali.fileservice = CLOUDFRONT
```

Note: The `LEGALI` file service is deprecated and only used for development.
For details, refer to `README-FILES.md`.

&nbsp;
&nbsp;

---

&nbsp;
&nbsp;

## Entities and Events

For detailed information about Entities and Events refer to JavaDoc or Swagger (https://agents.legal-i.ch/doc/swagger.html)

### Entity Metadata

The `LegalCase` and `SourceFiles` entities contain a metadata field to add integration-specific key-value store, with
type `string` / `string`. Defaults can be set in the application config or via environment variables.

Those property can be used to store arbitrary data, e.g. internal IDs. Further, this metadata is also used to override legal-i's processing pipeline the given source file.

Empty properties are considered as

```

# override the extracted title for this source file. if multiple documents are detected, it is used for all of them
legali.metadata.title = "Dokumenttitel" # value: string containing the title. default extracted value


# override the detected document type / label
legali.metadata.doctype = "type_medical_report" # value: one of the document types as string ('type_*'). default detected value

# overrides the issue date
legali.metadata.issuedate = "2020-01-01" # value: date in YYYY-MM-DD as string. default: extracted value

# disables splitting of this source file into documents
legali.pipeline.splitting.disabled = "true" # value: "true" or "false", passed as string. default: false

# the customer's internal document type, will be used for the upcoming mapping feature
legali.mapping.key = "InternerDokTyp128" # value as string

# DEBUG

# Disables the entire processing pipeline for this file (to test APIs)
legali.pipeline.disabled = "true" # value: "true" or "false", passed as string. default false

```

&nbsp;
&nbsp;

### Events

The agent must subscribe to the events it wants to receive. Events are retained for 3 days.

```
@PostConstruct
public void init() {
	this.eventService.subscribe(PongEvent.class, ExportPublishedEvent.class);
}
```

After subscribing, the Agent listens to events using eating event listeners. Every event listener must acknowledge its events. Events that are not acknowledged are sent again after 5 minutes.

```
@EventListener
public void handle(PongEvent event) {
	log.info("🏓 PingPong Event received: " + "\nid " + event.getUuid());

	// Acknowledge
	this.eventService.acknowledge(event)
}
```

*Events:*

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
Emitted when all source files of a legal case are successfully processed.

`SourceFileCreatedEvent`.
Emitted when a user creates a source file via the frontend.

`SourceFileUpdatedEvent`
Emitted when a user changes the folder in the frontend

`SourceFileReadyEvent`
Emitted when the pipeline is done processing a source file.

`SourceFileFailedEvent`
Emitted by the pipeline when processing of the source file failed.

`ExportCreatedEvent`
Emitted when a user creates a new export through the frontend.

`ExportSharedEvent`
Emitted when a user publishes a new export with a link or an email.

`ExportViewedEvent`
Emitted when an external user opens/downloads an exported pdf.

`NotebookUpdatedEvent`
Emitted when a user updates the legal case notebook.


## References
### Configuration and Deployment

All configurations can be set as environment variables by Spring Boot configuration convention.

````
# Example Agent Config
# Iterations and parallel threads
legali.example.iterations=1
spring.task.execution.pool.max-size=1
spring.task.execution.pool.core-size=1

# Run cleanup round to delete test legal cases
legali.example.cleanup=true

# Disable processing pipeline for development (do not use in production)
legali.default-metadata.legali.pipeline.disabled=true
legali.default-metadata.legali.uploader=example-agent

# Connection to the legal-i Cloud
legali.auth-url=https://auth.legal-i.ch
legali.api-url=https://agents.legal-i.ch/agents/v1
legali.client-id=<>
legali.client-secret=<>

# FileService: Use CLOUDFRONT to upload files directly to AWS.
# If there are network restrictions, you can use LEGALI to proxy via legal-i API. This is not recommended.
legali.fileservice=CLOUDFRONT

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
logging.level.ch.legali.sdk.services=INFO
logging.level.ch.legali.sdk.internal=INFO

# Debug HTTP connection
#logging.level.feign=DEBUG
#legali.feign-log-level=FULL

# Monitoring
server.port=8085
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
management.endpoint.health.group.readiness.include=readinessState,agent
````

### Folders (Aktenordner)

```
unknown                        : Anderes
internal_accident              : Eigene UVG Akten
internal_pension               : Eigene BVG Akten
internal_health_allowances     : Eigene KTG Akten
external_accident              : Anderer UVG Versicherer
external_pension               : Anderer BVG Versicherer
external_health_allowance      : Anderer KTG Versicherer
external_other                 : Andere Drittakten
health                         : Krankenversicherung
health_allowance               : Krankentaggeld (KVG und VVG)
liability                      : Haftpflichtrecht
army                           : Militärversicherung
pension                        : BVG
vvg                            : VVG
accident                       : Unfall
suva                           : Unfall (SUVA)

iv-ag                          : IV Aargau
iv-ai                          : IV Appenzell Innerrhoden
iv-ar                          : IV Appenzell Ausserrhoden
iv-be                          : IV Bern
iv-bl                          : IV Basel-Landschaft
iv-bs                          : IV Basel-Stadt
iv-fr                          : IV Freiburg
iv-ge                          : IV Genf
iv-gl                          : IV Glarus
iv-gr                          : IV Graubünden
iv-ju                          : IV Jura
iv-lu                          : IV Luzern
iv-ne                          : IV Neuenburg
iv-nw                          : IV Nidwalden
iv-ow                          : IV Obwalden
iv-sg                          : IV St. Gallen
iv-sh                          : IV Schaffhausen
iv-so                          : IV Solothurn
iv-sz                          : IV Schwyz
iv-tg                          : IV Thurgau
iv-ti                          : IV Tessin
iv-ur                          : IV Uri
iv-vd                          : IV Waadt
iv-vs                          : IV Wallis
iv-zg                          : IV Zug
iv-zh                          : IV Zürich

iv-li                          : IV Liechtenstein
```

### Document Types (Dokumenttypen / Labels)

```
type_admin    						: Admin
type_admin_auto                     : Admin (auto-recognize subtype if possible)
type_admin_claim_report_uvg         : Schaden- / Krankheitsmeldungen
type_admin_iv_registration          : IV Anmeldungen
type_admin_facts_sheet              : Feststellungsblätter
type_admin_table_of_content         : Inhaltsverzeichnisse
type_admin_protocol                 : Protokolle

type_correspondence                 : Korrespondenz
type_correspondence_auto            : Korrespondenz (auto-recognize subtype if possible)
type_correspondence_external_emails : Externe Emails
type_correspondence_internal_emails : Interne Emails
type_correspondence_letters         : Briefe

type_medical                        : Medizinisch
type_medical_auto                   : Medizinisch (auto-recognize subtype if possible)
type_medical_certificate            : AUF-Zeugnisse
type_medical_report                 : Arztberichte
type_medical_insurance_report       : Vers. interne Arztberichte
type_medical_prescription           : Med. Verordnungen
type_medical_cost_credit            : Kostengutsprachen
type_medical_expert_opinion         : Gutachten
type_medical_form                   : Formular

type_legal                          : Rechtlich
type_legal_auto                     : Rechtlich (auto-recognize subtype if possible)
type_legal_pre_disposal             : Vorbescheide / Formlose Ablehnungen
type_legal_disposal                 : Verfügungen
type_legal_objection                : Einsprachen
type_legal_objection_decision       : Einsprache-Entscheide
type_legal_attorney_submission      : Anwaltliche Eingaben
type_legal_court_decision           : Urteile
type_legal_proxy                    : Vollmachten
type_legal_submissions_court        : Eingaben Gerichtsverfahren
type_legal_criminal_file            : Strafakten

type_profession                     : Berufliches
type_profession_auto                : Berufliches (auto-recognize subtype if possible)
type_profession_ik_statement        : IK-Auszüge
type_profession_cv                  : Lebensläufe
type_profession_employment_contract : Arbeitsverträge
type_profession_questionnaire       : Arbeitgeberfragebogen
type_profession_wage_statements     : Lohnabrechnungen

type_financial                      : Finanziell
type_financial_auto                 : Finanziell (auto-recognize subtype if possible)
type_financial_allowance_overview   : Taggeld-Abrechnungen
type_financial_invoice              : Rechnungen


type_internal                       : Interne Dokumente
type_internal_antifraud             : Akten der internen Betrugsbekämpfungsstelle
type_internal_reports               : Interne Berichte

type_recourse                       : Regress

type_other                          : Andere / Übriges
type_other_phone_memo               : Telefon- / Aktennotizen

```
