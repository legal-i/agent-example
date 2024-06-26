# legal-i Agent Examples

## Overview

- The legal-i Example Agents are sample applications showcasing how to build a customer-specific agent.
- The legal-i SDK ensures solid two-way communication between the customers' environment and the legal-i cloud.
- The latest version is available on legal-i's GitHub repository.
# No-Code Quickstart

**Prerequisites**

- Access to legal-i's GitHub repository at [https://github.com/legal-i/agent-example](https://github.com/legal-i/agent-example)
- Agent credentials for your workspace, available through the app in the workspace settings under "Integration"
- Recent versions of Docker and Docker Compose
- Internet access to `*.legal-i.ch`

**Steps:**

1. Set up legal-i cloud credentials in `quickstart/agent.env`
2. In the `quickstart` directory, run `docker-compose build`.
3. Run `docker-compose up agent_spring` or `docker-compose up agent_quarkus`
4. The agent runs and starts exchanging data
5. On the app, check the agent status in Workspace Settings / Integration.
6. In case you need monitoring or proxy support, you can start the corresponding services:
   - Monitoring: `docker-compose up prometheus grafana`
     - Access Grafana at [http://localhost:3000/](http://localhost:3000/) (admin/adminex)
     - Add Prometheus data source pointing to http://prometheus:9090/
   - HTTP Proxy: `docker-compose up squid` and adapt `agent.env` to use the proxy
## Development

See the READMEs in the framework-specific subdirectories for details.

### Credentials
Make sure to set the secrets correctly via environment variables or a properties file:

```
LEGALI_API_URL=https://{customer-prefix}.agents.legal-i.ch/agents/v1
LEGALI_AUTH_URL=https://auth.legal-i.ch/
LEGALI_CLIENT_ID=<from workspace>
LEGALI_CLIENT_SECRET=<from workspace>
```

## Agent Authorization
- The legal-i SDK is authorized via legal-i's IDP `https://auth.legal-i.ch` using the OAuth 2.0 Client Credentials Grant.
- Client credentials can be rotated by workspace admins in the legal-i app.

&nbsp;
## Protocols and Firewall
- The legal-i SDK communicates via HTTPS/TLS1.4 to REST endpoints
- Outbound access is required to
	- the environment-specific agent endpoints, i.e., `https://{customer-prefix}.agents.legal-i.ch`
		- For OpenAPI3 specs refer to `https://agents.legal-i.ch/doc/swagger.html`
	- legal-i's data buckets on AWS the following URLs depending on your data region.
		- Data Ingestion `https://upload.legal-i.ch/*`
		- Data Region in CH `https://data-ch.legal-i.ch/*` or
		- Data Region in DE `https://data.legal-i.ch/*`
	- legal-i's IDP: `https://auth.legal-i.ch/oauth/token`
- No inbound access is required.

&nbsp;

### File Transfer API
The SDK transfers files directly from and to AWS using presigned URLs. Therefore, the following extra
endpoints must be accessible outbound:
```
# The files are transferred to these endpoints:
PUT https://upload.legal-i.ch/{any}
GET https://data-ch.legal-i.ch/{any} (CH)
GET https://data.legal-i.ch/{any} (DE)
```
```
# Enabled
legali.fileservice = CLOUDFRONT
```

Note: The `LEGALI` file service is deprecated and only used for development.
For details, refer to `README-FILES.md`.


## Entities, Events and Mapping

For detailed information about Entities and Events refer to Swagger (https://agents.legal-i.ch/doc/swagger-ui/index.html)

### Entity Metadata

The `LegalCase` and `SourceFile` entities contain a metadata field to add integration-specific key-value pairs, with
type `string` / `string`. Defaults can be set in the application config or via environment variables.

Those properties are used to store arbitrary data, e.g. internal IDs. Further, this metadata is also used to override legal-i's processing pipeline the given source file. Empty strings in properties are considered as not set.

### Override legal-i's processing pipeline
A customer might have predefined document types that should not be processed by legal-i's extraction and classification pipeline. In this case, a mapping key is passed upon creation of the sourcefile.
```
legali.mapping.key = "a5bf"
```
legal-i checks the mapping configuration for a corresponding entry. If such entry has been configured in the legal-i app, the defined rules for the document's folder, label, whether it is split and which issue date is chosen are applied.

Further, the following properties can be used to override processing. If those properties are not set or empty, the data extracted by legal-i is used.
```
# override the extracted document title
legali.metadata.title = "Dokumenttitel"

# overrides the issue date
legali.metadata.issuedate = "2020-01-22" (NOTE: YYYY-MM-DD)

# sets a receipt date on the document
legali.metadata.receiptdate = "2020-01-01" (NOTE: YYYY-MM-DD)

# For debugging: the pipeline can be disabled entirely
legali.pipeline.disabled = "true"
```

The following properties can also be sent directly on the SourceFile. This approach is deprecated and shall not be used in future integrations.
```
# override the detected document type / label
legali.metadata.doctype = "type_medical_report"

# disables splitting of the source file into documents
legali.pipeline.splitting.disabled = "true"
```
### Example for setting up Document Type classifications

Document Type / Labels:

- To have legal-i classify the type, *do not send this property* or send an *empty string* as the value.
- If you know the exact document type, pass it, e.g., `type_medical_report`.
- If you know the parent type and want legal-i to classify the subtype, pass `type_parent_auto`. For example, `type_medical_auto` will limit the classification to all medical subtypes.
- If you don’t know the exact type and *do not want legal-i to classify the document*, pass `type_other`.

This behavior can also be configured in the mapping configuration in the legal-i app.


### Events

The agent must subscribe to the events it wants to receive. Events are retained for 3 days.
A list of all events can be found on swagger https://agents.legal-i.ch/doc/swagger-ui/index.html

## References
### Case Data 
```
PII_LASTNAME                   : Name
PII_FIRSTNAME                  : Vorname
PII_COMPANY                    : Kunde
PII_GENDER                     : Geschlecht
PII_BIRTHDATE                  : Geburtsdatum
PII_AHV_NR                     : AHV-Nummer
PII_MARITAL_STATUS             : Zivilstand
PII_NATIONALITY                : Staatsbürgerschaft
PII_ADDRESS                    : Adresse, Postleitzahl, Ort
PII_PLZ                        : Postleitzahl
PII_PHONE_NO                   : Telefonnummer 

JOB_TITLE                      : Beruf
JOB_ISCO_CODE                  : Berufsbezeichung (normalisiert nach ISCO Klassifikation)
JOB_POSITION                   : Position
JOB_COMPETENCE_LEVEL           : Kompetenzstufe
JOB_PHYSICAL_LOAD              : Körperliche Belastung
JOB_TIME_PERCENTAGE            : Beschäftigungsgrad
JOB_INCOME_YEAR                : Jahreseinkommen

INCIDENT_DATE                  : Ereignisdatum
INCIDENT_ICD10_CODE            : Ereignis ICD-10-Code
INCIDENT_DESCRIPTION           : Unfallbeschreibung / Beschreibung Krankheit 
INCIDENT_POLICE_NO             : Policennummer
INCIDENT_KIND                  : Ereignisart (Unfall oder Krankheit)
INCIDENT_BODY_PART             : Betroffener Körperteil 
INCIDENT_BODY_PART_ICD10_CODE  : Betroffener Körperteil ICD-10-Code
INCIDENT_DISPUTE_VALUE         : Streitwert (in CHF)

CUSTOM_1                       : Benutzerdefiniertes Feld 1  (Additional reference)
CUSTOM_2                       : Benutzerdefiniertes Feld 2
CUSTOM_3                       : Benutzerdefiniertes Feld 3
```

### Folders (Aktenordner)

```
unknown                        : Anderes
migration_file                 : Migrationsdossier
internal_files                 : Eigene Akten
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
social_insurance               : Sozialversicherungsakten
civil_process                  : Prozessakten (ZPO)
social_process                 : Prozessakten (Soz.)
legal_representation_files     : Akten Rechtsvertretung

iv                             : IV Allgemein
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
type_admin                          : Admin
type_admin_auto                     : Admin (auto-recognize subtype if possible)
type_admin_claimreportuvg           : Schaden- / Krankheitsmeldungen
type_admin_ivregistration           : IV Anmeldungen
type_admin_factssheet               : Feststellungsblätter
type_admin_tableofcontent           : Inhaltsverzeichnisse
type_admin_protocol                 : Protokolle
type_admin_supplementaryahvivpayments: Verrechnung Nachzahlungen AHV/IV
type_admin_reportingalviv           : Meldeverfahren ALV/IV

type_correspondence                 : Korrespondenz
type_correspondence_auto            : Korrespondenz (auto-recognize subtype if possible)
type_correspondence_email           : Emails
type_correspondence_email_internal  : Interne Emails
type_correspondence_email_admin     : Administrative E-Mails
type_correspondence_email_medical   : Medizinische E-Mails
type_correspondence_email_legal     : Rechtliche E-Mails
type_correspondence_email_profession: Berufliche E-Mails
type_correspondence_email_financial : Finanzielle E-Mails
type_correspondence_letter          : Briefe
type_correspondence_phonememo       : Telefon- / Aktennotizen

type_medical                        : Medizinisch
type_medical_auto                   : Medizinisch (auto-recognize subtype if possible)
type_medical_certificate            : AUF-Zeugnisse
type_medical_report                 : Arztberichte
type_medical_report_surgical        : Operationsberichte
type_medical_report_progress        : Verlaufsberichte
type_medical_report_radiological    : Radiologieberichte
type_medical_report_ discharge      : Austrittsberichte
type_medical_report_lab             : Laborberichte
type_medical_report_insurance       : Vers. interne Arztberichte
type_medical_prescription           : Med. Verordnungen
type_medical_costcredit             : Kostengutsprachen
type_medical_expertopinion          : Gutachten

type_legal                          : Rechtlich
type_legal_auto                     : Rechtlich (auto-recognize subtype if possible)
type_legal_predisposal              : Vorbescheide / Formlose Ablehnungen
type_legal_disposal                 : Verfügungen
type_legal_requestforfile           : Akteneinsichtsgesuche
type_legal_objection                : Einsprachen / Einwände
type_legal_objectiondecision        : Einsprache-Entscheide
type_legal_attorneysubmission       : Anwaltliche Korrespondenz
type_legal_courtdecision            : Urteile
type_legal_proxy                    : Vollmachten
type_legal_submissionscourt         : Eingaben Gerichtsverfahren
type_legal_criminalfile             : Strafakten

type_profession                     : Berufliches
type_profession_auto                : Berufliches (auto-recognize subtype if possible)
type_profession_ikstatement         : IK-Auszüge
type_profession_cv                  : Lebensläufe
type_profession_employmentcontract  : Arbeitsverträge / Kündigungen
type_profession_questionnaire       : Arbeitgeberfragebogen
type_profession_wagestatements      : Lohnabrechnungen
type_profession_reference           : Arbeitszeugnisse / Diplome
type_profession_integration         : Eingliederung

type_financial                      : Finanziell
type_financial_auto                 : Finanziell (auto-recognize subtype if possible)
type_financial_allowanceoverview    : Taggeld-Abrechnungen
type_financial_invoice              : Rechnungen
type_internal                       : Interne Dokumente
type_internal_antifraud             : Akten der internen Betrugsbekämpfungsstelle
type_internal_report                : Interne Berichte

type_recourse                       : Regress

type_other                          : Andere / Übriges (Disables legal-i's classification)
```
