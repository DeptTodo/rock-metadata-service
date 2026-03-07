# Rock Metadata Service

A metadata management service that automatically crawls and catalogs database schemas, with AI tool integration via MCP (Model Context Protocol). Built with Spring Boot and [SchemaCrawler](https://www.schemacrawler.com/).

## Features

- **Multi-database support** — PostgreSQL, MySQL, Oracle, SQL Server, SQLite
- **Async metadata crawling** — register a datasource, trigger a crawl, and query the results via REST API or MCP tools
- **Full schema introspection** — tables, columns, primary keys, foreign keys, indexes, triggers, constraints, privileges, routines, sequences
- **Data dictionary management** — define dictionaries, manage items, bind to columns
- **Metadata tagging** — attach key-value tags to schemas, tables, and columns
- **Schema diff** — compare two crawl snapshots to detect added/removed/modified tables and columns
- **FK relationship traversal** — BFS graph traversal with cascade impact analysis
- **Advanced search** — multi-criteria filtering on tables and columns with JPA Specifications
- **Data profiling** — live database profiling (distinct count, null count, min/max, sample values)
- **Metadata export** — export as DDL, JSON, or Markdown documentation
- **Health & monitoring** — freshness checking, connection test, live vs crawled table count comparison
- **53 MCP tools** — full AI agent integration via Spring AI MCP Server (SSE at `/mcp/message`)

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

### LLM Analysis Jobs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/llm-analysis-jobs` | List jobs (`?datasourceId=`) |
| `GET` | `/api/llm-analysis-jobs/{id}` | Get job detail |

### SQL Execution

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/sql/execute` | Execute SQL against a datasource |

## MCP Tools (53 tools)

The service exposes an MCP server via SSE at `/mcp/message` with the following tool groups:

| Tool Group | Count | Description |
|------------|-------|-------------|
| DataSourceTools | 7 | Datasource CRUD + connection testing |
| CrawlTools | 3 | Trigger crawls, check status |
| MetadataTools | 16 | Schema/table/column/routine/sequence queries, export, summary, health, advanced search |
| SqlTools | 1 | Execute SQL |
| TagTools | 6 | Tag CRUD |
| DictTools | 14 | Dictionary definitions, items, bindings CRUD |
| LlmAnalysisTools | 2 | LLM analysis job queries |
| SchemaDiffTools | 1 | Schema change detection between crawls |
| RelationshipTools | 2 | FK graph traversal, impact analysis |
| ProfilingTools | 2 | Live data profiling |

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

## Project Structure

```
src/main/java/com/rock/metadata/
├── MetadataServiceApplication.java
├── controller/
│   ├── DataSourceController.java      # Datasource CRUD + connection test
│   ├── CrawlController.java           # Crawl trigger/status
│   ├── MetadataController.java        # Metadata query, export, summary, health, diff, relationships, search, profiling
│   ├── TagController.java             # Tag CRUD
│   ├── DictController.java            # Dictionary CRUD
│   └── SqlExecuteController.java      # SQL execution
├── service/
│   ├── CrawlService.java              # Async crawl + SchemaCrawler integration
│   ├── MetadataQueryService.java      # Metadata reads + advanced search
│   ├── SqlExecuteService.java         # SQL execution against target DBs
│   ├── TagService.java                # Tag CRUD logic
│   ├── DictService.java               # Dictionary management
│   ├── ConnectionTestService.java     # JDBC connection testing
│   ├── MetadataExportService.java     # DDL/JSON/Markdown export
│   ├── DatasourceSummaryService.java  # Statistics & overview
│   ├── MetadataHealthService.java     # Freshness & health checking
│   ├── SchemaDiffService.java         # Schema change detection
│   ├── RelationshipService.java       # FK traversal & impact analysis
│   ├── DataProfilingService.java      # Live data profiling
│   └── JdbcUrlBuilder.java            # JDBC URL construction utility
├── mcp/
│   ├── McpServerConfig.java           # Registers 10 tool providers
│   └── tool/                          # 10 @Tool classes (53 tools total)
├── model/                             # 19 JPA entities
├── dto/                               # 18 request/response DTOs
└── repository/
    ├── ...Repository.java             # 19 Spring Data JPA repositories
    └── spec/                          # JPA Specifications for advanced search
```

## License

[MIT](LICENSE)
