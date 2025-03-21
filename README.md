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
# sets a receipt date on the document
legali.metadata.receiptdate = "2020-01-01" (NOTE: YYYY-MM-DD)

# sets the alternative title on the document (NOTE: updatable metadata field)
legali.metadata.alttitle = "alternative title"

# sets the pagination number on the document (NOTE: updatable metadata field)
legali.metadata.pagination.number = "123"

# sets the pagination id on the document (NOTE: updatable metadata field)
legali.metadata.pagination.id = "Wf0ZoNA5"

# For debugging: the pipeline can be disabled entirely
legali.pipeline.disabled = "true"
```


### Events

The agent must subscribe to the events it wants to receive. Events are retained for 3 days.
A list of all events can be found on swagger https://agents.legal-i.ch/doc/swagger-ui/index.html
