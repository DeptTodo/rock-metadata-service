# Rock Metadata Service

A metadata management service that automatically crawls and catalogs database schemas, with AI tool integration via MCP (Model Context Protocol). Built with Spring Boot and [SchemaCrawler](https://www.schemacrawler.com/).

## Features

- **Multi-database support** — PostgreSQL, MySQL, Oracle, SQL Server, SQLite
- **Async metadata crawling** — register a datasource, trigger a crawl, and query the results via REST API or MCP tools
- **Full schema introspection** — tables, columns, primary keys, foreign keys, indexes, triggers, constraints, privileges, routines, sequences
- **Metadata annotation** — update business attributes (display name, description, owner, domain, importance) on schemas, tables, and columns, with sensitivity/compliance tagging for columns
- **Data dictionary management** — define dictionaries, manage items, bind to columns
- **Metadata tagging** — attach key-value tags to schemas, tables, and columns
- **Data quality rules** — define reusable quality rules (NOT_NULL, UNIQUE, VALUE_RANGE, REGEX_MATCH, CUSTOM_SQL, etc.), bind to columns, execute live checks with violation reporting
- **Schema diff** — compare two crawl snapshots to detect added/removed/modified tables and columns
- **FK relationship traversal** — BFS graph traversal with cascade impact analysis
- **Advanced search** — multi-criteria filtering on tables and columns with JPA Specifications
- **Data profiling** — live database profiling (distinct count, null count, min/max, sample values, distinct value frequency)
- **Data sampling** — sample rows and distinct column values from live databases
- **Metadata export** — export as DDL, JSON, or Markdown documentation
- **Health & monitoring** — freshness checking, connection test, live vs crawled table count comparison
- **70 MCP tools** — full AI agent integration via Spring AI MCP Server (SSE at `/sse`)
- **Claude Code integration** — `.mcp.json` config for direct use as Claude Code MCP client

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.4.3 |
| Spring AI MCP Server | 1.0.0-M6 |
| SchemaCrawler | 17.7.0 |
| PostgreSQL (metadata store) | 16+ |

## Quick Start

### Docker Compose (recommended)

```bash
docker compose up -d
```

This starts both PostgreSQL and the application. The service will be available at `http://localhost:9990`.

### Manual Setup

**Prerequisites:** Java 21+, Maven 3.9+, PostgreSQL

```sql
CREATE DATABASE rock_metadata;
```

```bash
mvn clean compile
mvn spring-boot:run
```

The service starts on `http://localhost:9990`.

### Claude Code Integration

The project includes a `.mcp.json` file that registers the MCP server for use with [Claude Code](https://docs.anthropic.com/en/docs/claude-code). Once the service is running, Claude Code can directly use all 70 MCP tools for metadata management.

```json
{
  "mcpServers": {
    "rock-metadata": {
      "type": "sse",
      "url": "http://localhost:9990/sse?api_key=YOUR_API_KEY"
    }
  }
}
```

Set the `MCP_API_KEY` environment variable (or `metadata.mcp.api-key` in `application.yml`) to enable API key authentication. If not set, authentication is disabled.

## API Reference

### Datasource Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/datasources` | Register a datasource |
| `GET` | `/api/datasources` | List all datasources |
| `GET` | `/api/datasources/{id}` | Get datasource detail |
| `PUT` | `/api/datasources/{id}` | Update a datasource |
| `DELETE` | `/api/datasources/{id}` | Delete a datasource |
| `POST` | `/api/datasources/{id}/test-connection` | Test datasource connectivity |
| `POST` | `/api/datasources/test-connection` | Test ad-hoc connection |

### Crawl Jobs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/datasources/{id}/crawl` | Trigger a metadata crawl |
| `GET` | `/api/crawl-jobs` | List crawl jobs (`?datasourceId=`) |
| `GET` | `/api/crawl-jobs/{id}` | Get crawl job status |

### Metadata Query

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/datasources/{id}/schemas` | List schemas |
| `GET` | `/api/datasources/{id}/tables` | List tables (`?schema=`) |
| `GET` | `/api/datasources/{id}/search?keyword=` | Search tables & columns |
| `GET` | `/api/datasources/{id}/routines` | List routines (`?schema=`) |
| `GET` | `/api/datasources/{id}/sequences` | List sequences (`?schema=`) |
| `GET` | `/api/datasources/{id}/table-row-counts` | Count table rows (`?schema=&tableName=`) |
| `GET` | `/api/tables/{id}` | Full table detail (columns, PKs, FKs, indexes, triggers, constraints, privileges) |
| `GET` | `/api/tables/{id}/columns` | List columns |
| `GET` | `/api/tables/{id}/foreign-keys` | List foreign keys |
| `GET` | `/api/tables/{id}/indexes` | List indexes |
| `GET` | `/api/routines/{id}` | Routine detail with parameters |

### Metadata Annotation

| Method | Endpoint | Description |
|--------|----------|-------------|
| `PATCH` | `/api/schemas/{id}/attrs` | Update schema business attributes (displayName, businessDescription, owner) |
| `PATCH` | `/api/tables/{id}/attrs` | Update table business attributes (displayName, businessDescription, businessDomain, owner, importanceLevel, dataQualityScore) |
| `PATCH` | `/api/columns/{id}/attrs` | Update column attributes (displayName, businessDescription, businessDataType, sampleValues, valueRange, sensitivityLevel, sensitivityType, maskingStrategy, complianceFlags) |

### Advanced Analysis

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/datasources/{id}/summary` | Dashboard-style overview (counts, distributions, top tables) |
| `GET` | `/api/datasources/{id}/health` | Health check (freshness, connectivity, table count comparison) |
| `GET` | `/api/datasources/{id}/export?format=` | Export metadata (DDL / JSON / MARKDOWN) |
| `GET` | `/api/datasources/{id}/diff` | Schema diff between crawls (`?crawlJobId1=&crawlJobId2=`) |
| `POST` | `/api/datasources/{id}/advanced-search` | Multi-criteria search (body: filters) |
| `GET` | `/api/tables/{id}/relationships` | FK relationship graph (`?depth=`) |
| `GET` | `/api/tables/{id}/impact-analysis` | Cascade impact analysis |
| `GET` | `/api/datasources/{dsId}/tables/{tId}/profile` | Table profiling (`?columns=`) |
| `GET` | `/api/datasources/{dsId}/tables/{tId}/profile/{col}` | Column profiling |

### Tags

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/tags` | Create a tag |
| `GET` | `/api/tags/by-target?targetType=&targetId=` | List tags by target |
| `GET` | `/api/tags/by-key?tagKey=&tagValue=` | List tags by key |
| `PUT` | `/api/tags/{id}` | Update a tag |
| `DELETE` | `/api/tags/{id}` | Delete a tag |
| `DELETE` | `/api/tags/by-target?targetType=&targetId=` | Delete all tags of a target |

### Data Dictionaries

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/dicts` | Create a dictionary |
| `GET` | `/api/dicts` | List dictionaries (`?datasourceId=&activeOnly=`) |
| `GET` | `/api/dicts/{id}` | Dict detail with items |
| `GET` | `/api/dicts/by-code/{code}` | Dict detail by code |
| `PUT` | `/api/dicts/{id}` | Update a dictionary |
| `DELETE` | `/api/dicts/{id}` | Delete a dictionary |
| `POST` | `/api/dicts/items` | Add dict item |
| `GET` | `/api/dicts/{id}/items` | List dict items (`?activeOnly=`) |
| `PUT` | `/api/dicts/items/{id}` | Update dict item |
| `DELETE` | `/api/dicts/items/{id}` | Delete dict item |
| `POST` | `/api/dicts/bindings` | Bind dict to column |
| `GET` | `/api/dicts/{id}/bindings` | List dict bindings |
| `GET` | `/api/dicts/bindings/by-column/{colId}` | List column bindings |
| `DELETE` | `/api/dicts/bindings/{id}` | Delete binding |

### Data Quality Rules

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/quality/rules` | Create a quality rule |
| `GET` | `/api/quality/rules` | List rules (`?ruleType=&activeOnly=`) |
| `GET` | `/api/quality/rules/{id}` | Get rule detail |
| `PUT` | `/api/quality/rules/{id}` | Update a rule |
| `DELETE` | `/api/quality/rules/{id}` | Delete a rule |
| `POST` | `/api/quality/column-rules` | Bind rule to column |
| `GET` | `/api/quality/column-rules` | List column rules (`?datasourceId=&schemaName=&tableName=&columnName=`) |
| `GET` | `/api/quality/column-rules/by-meta-column/{id}` | List rules by MetaColumn |
| `PUT` | `/api/quality/column-rules/{id}` | Update column rule binding |
| `DELETE` | `/api/quality/column-rules/{id}` | Delete column rule binding |
| `POST` | `/api/quality/check/column` | Execute quality check on a column |
| `POST` | `/api/quality/check/table` | Execute quality check on all columns of a table |

### LLM Analysis Jobs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/llm-analysis-jobs` | List jobs (`?datasourceId=`) |
| `GET` | `/api/llm-analysis-jobs/{id}` | Get job detail |

### SQL Execution

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/sql/execute` | Execute SQL against a datasource |

## MCP Tools (70 tools)

The service exposes an MCP server via SSE at `/sse` (message endpoint `/mcp/message`) with the following tool groups:

| Tool Group | Count | Description |
|------------|-------|-------------|
| DataSourceTools | 7 | Datasource CRUD + connection testing |
| CrawlTools | 3 | Trigger crawls, check status |
| MetadataTools | 15 | Schema/table/column/routine/sequence queries, export, summary, health, advanced search |
| SqlTools | 1 | Execute SQL |
| TagTools | 6 | Tag CRUD |
| DictTools | 14 | Dictionary definitions, items, bindings CRUD |
| LlmAnalysisTools | 2 | LLM analysis job queries |
| SchemaDiffTools | 1 | Schema change detection between crawls |
| RelationshipTools | 2 | FK graph traversal, impact analysis |
| ProfilingTools | 4 | Live data profiling, sampling, distinct values |
| AnnotationTools | 3 | Schema/table/column business attribute updates |
| DataQualityTools | 12 | Quality rule CRUD, column rule bindings, live quality checks |

## Example: Register and Crawl

```bash
# Register datasource
curl -X POST http://localhost:9990/api/datasources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_database",
    "dbType": "postgresql",
    "host": "localhost",
    "port": 5432,
    "databaseName": "my_database",
    "username": "postgres",
    "password": "postgres"
  }'

# Test connection
curl -X POST http://localhost:9990/api/datasources/1/test-connection

# Trigger crawl
curl -X POST http://localhost:9990/api/datasources/1/crawl \
  -H "Content-Type: application/json" \
  -d '{"infoLevel": "maximum"}'

# Check crawl status
curl http://localhost:9990/api/crawl-jobs/1

# Browse metadata
curl http://localhost:9990/api/datasources/1/schemas
curl http://localhost:9990/api/datasources/1/tables
curl http://localhost:9990/api/tables/1

# Get summary dashboard
curl http://localhost:9990/api/datasources/1/summary

# Export as DDL
curl "http://localhost:9990/api/datasources/1/export?format=DDL"

# Health check
curl http://localhost:9990/api/datasources/1/health
```

## Crawl Info Levels

| Level | Description |
|-------|-------------|
| `minimum` | Table names only |
| `standard` | Tables + columns |
| `detailed` | Tables + columns + keys + indexes |
| `maximum` | Everything including definitions, remarks, routines, sequences |

## Configuration

Key settings in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 9990 | HTTP port |
| `metadata.crawl.thread-pool-size` | 5 | Async crawl thread pool size |
| `metadata.crawl.retain-count` | 2 | Number of successful crawl snapshots to retain (for schema diff) |
| `metadata.mcp.api-key` | _(empty)_ | MCP API key for SSE authentication (env: `MCP_API_KEY`) |

## Project Structure

```
src/main/java/com/rock/metadata/
├── MetadataServiceApplication.java
├── controller/
│   ├── DataSourceController.java          # Datasource CRUD + connection test
│   ├── CrawlController.java              # Crawl trigger/status
│   ├── MetadataController.java           # Metadata query, export, summary, health, diff, relationships, search, profiling
│   ├── MetadataAnnotationController.java  # Schema/table/column business attribute updates
│   ├── DataQualityController.java         # Quality rule CRUD, column rule bindings, quality checks
│   ├── TagController.java                # Tag CRUD
│   ├── DictController.java               # Dictionary CRUD
│   └── SqlExecuteController.java         # SQL execution
├── service/
│   ├── CrawlService.java                 # Async crawl + SchemaCrawler integration
│   ├── MetadataQueryService.java         # Metadata reads + advanced search
│   ├── MetadataAnnotationService.java    # Business attribute updates for schemas/tables/columns
│   ├── DataQualityService.java           # Quality rule management + live quality checks
│   ├── SqlExecuteService.java            # SQL execution against target DBs
│   ├── TagService.java                   # Tag CRUD logic
│   ├── DictService.java                  # Dictionary management
│   ├── ConnectionTestService.java        # JDBC connection testing
│   ├── MetadataExportService.java        # DDL/JSON/Markdown export
│   ├── DatasourceSummaryService.java     # Statistics & overview
│   ├── MetadataHealthService.java        # Freshness & health checking
│   ├── SchemaDiffService.java            # Schema change detection
│   ├── RelationshipService.java          # FK traversal & impact analysis
│   ├── DataProfilingService.java         # Live data profiling
│   └── JdbcUrlBuilder.java              # JDBC URL construction utility
├── mcp/
│   ├── McpServerConfig.java              # Registers 12 tool providers
│   └── tool/                             # 12 @Tool classes (70 tools total)
├── model/                                # 21 JPA entities + 7 enums
├── dto/                                  # 33 request/response DTOs
└── repository/
    ├── ...Repository.java                # 21 Spring Data JPA repositories
    └── spec/                             # JPA Specifications for advanced search
```

## License

[MIT](LICENSE)
