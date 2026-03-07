# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
mvn clean compile

# Run (requires PostgreSQL with database `rock_metadata` on localhost:5432)
mvn spring-boot:run

# Package (skip tests)
mvn package -DskipTests

# Run tests
mvn test

# Docker (includes PostgreSQL)
docker compose up -d

# Rebuild & restart containers
docker compose up -d --build
```

The service runs on port **9990**. PostgreSQL is used as the metadata store (configured in `src/main/resources/application.yml`). Hibernate `ddl-auto: update` handles schema creation automatically.

## MCP Integration

This service is registered as an MCP server for Claude Code via `.mcp.json`. The SSE endpoint is `/sse` with message endpoint `/mcp/message`. API key authentication is controlled by `metadata.mcp.api-key` (env: `MCP_API_KEY`). When the service is running, all 90 MCP tools are available for direct use.

## Architecture

This is a Spring Boot 3.4 / Java 21 service that crawls external database schemas using [SchemaCrawler](https://www.schemacrawler.com/) and stores the resulting metadata in PostgreSQL. It exposes both a REST API and an MCP (Model Context Protocol) server for AI tool integration.

### Core Flow

1. **Register a datasource** (`DataSourceConfig`) with connection details
2. **Trigger an async crawl** (`CrawlService.executeCrawl`) — SchemaCrawler connects to the target database, introspects the schema, and the results are persisted as `Meta*` entities
3. **Query metadata** via REST endpoints or MCP tools — always reads from the latest successful crawl job
4. **Annotate metadata** — update business attributes (display name, description, owner, domain, sensitivity) on schemas, tables, and columns
5. **Manage metadata** — tag entities, define data dictionaries, bind dicts to columns
6. **Quality rules** — define quality rules, bind to columns, execute live quality checks with violation reporting
7. **Analyze** — diff schema changes, traverse FK relationships, profile live data
8. **Dataset engine** — define reusable dataset templates (nodes, relations, field mappings, transforms), publish with versioning, async execute with topological ordering, store result snapshots

### Key Packages (`com.rock.metadata`)

- **`model/`** — JPA entities (29) and enums (9). `DataSourceConfig` stores connection info. `CrawlJob` tracks crawl status/timing. `Meta*` entities (MetaSchema, MetaTable, MetaColumn, MetaPrimaryKey, MetaForeignKey, MetaIndex, MetaTrigger, MetaConstraint, MetaPrivilege, MetaRoutine, MetaRoutineColumn, MetaSequence) hold crawled metadata. `MetaTag` for tagging. `DictDefinition`, `DictItem`, `DictColumnBinding` for data dictionaries. `QualityRule`, `ColumnQualityRule` for data quality. `LlmAnalysisJob` for LLM analysis tracking. `DatasetDefinition`, `DatasetNode`, `DatasetNodeRelation`, `DatasetNodeFilter`, `DatasetFieldMapping`, `DatasetTransformRule`, `DatasetInstance`, `DatasetInstanceSnapshot` for the dataset engine. Enums: `CrawlStatus`, `ImportanceLevel`, `SensitivityLevel`, `DictSourceType`, `DictType`, `QualityRuleType`, `RuleSeverity`, `DatasetStatus`, `DatasetExecutionStatus`.
- **`service/`** — Business logic:
  - `CrawlService` — async crawls via `@Async`, persists SchemaCrawler `Catalog`, retains last N crawls (configurable via `metadata.crawl.retain-count`)
  - `MetadataQueryService` — reads metadata scoped to latest successful crawl, advanced search with JPA Specifications
  - `MetadataAnnotationService` — update business/security attributes on schemas, tables, and columns
  - `DataQualityService` — quality rule CRUD, column rule bindings, live quality check execution against target databases
  - `SqlExecuteService` — runs arbitrary SQL against registered datasources
  - `TagService` — CRUD for metadata tags
  - `DictService` — CRUD for data dictionaries, items, and column bindings
  - `ConnectionTestService` — JDBC connection testing with timeout
  - `MetadataExportService` — export as DDL/JSON/MARKDOWN
  - `DatasourceSummaryService` — dashboard-style overview with statistics
  - `MetadataHealthService` — freshness checking, connection testing, live table count comparison
  - `SchemaDiffService` — compare two crawls to detect schema changes
  - `RelationshipService` — FK graph traversal (BFS) and cascade impact analysis
  - `DataProfilingService` — live database profiling (distinct/null/min/max/samples)
  - `JdbcUrlBuilder` — static utility for multi-DB JDBC URL construction
  - `DatasetDefinitionService` — dataset template CRUD, node/relation/filter/fieldMapping/transformRule management, validation (Kahn's algorithm cycle detection), lifecycle (publish/archive)
  - `DatasetExecutionService` — async dataset execution engine: topological sort, per-node JDBC query, transform application, tree/flat assembly, snapshot storage, concurrent execution guard
  - `DatasetTransformEngine` — static utility for SQL expression building, dict lookup, format transforms, dangerous SQL keyword detection, multi-DB LIMIT/TOP
- **`controller/`** — REST endpoints under `/api/`:
  - `DataSourceController` — datasource CRUD + connection test
  - `CrawlController` — trigger/status crawl jobs
  - `MetadataController` — query metadata, routines, sequences, export, summary, health, diff, relationships, advanced search, profiling
  - `MetadataAnnotationController` — schema/table/column business attribute updates (`PATCH /api/{schemas|tables|columns}/{id}/attrs`)
  - `DataQualityController` — quality rule CRUD, column rule bindings, quality checks (`/api/quality/`)
  - `TagController` — tag CRUD (`/api/tags`)
  - `DictController` — dictionary CRUD (`/api/dicts`)
  - `SqlExecuteController` — execute SQL
  - `DatasetController` — dataset definition CRUD, nodes, relations, filters, field mappings, transform rules, execution, instances (`/api/datasets/`)
- **`mcp/`** — Spring AI MCP Server integration. `McpServerConfig` registers 13 tool providers. Tool classes in `mcp/tool/` wrap the same services for AI agent consumption. SSE endpoint at `/sse`, message endpoint at `/mcp/message`.
  - `DataSourceTools` (7 tools) — CRUD + connection test
  - `CrawlTools` (3 tools) — trigger, status, list
  - `MetadataTools` (15 tools) — schemas, tables, columns, FKs, indexes, routines, sequences, search, export, summary, health, advanced search, row counts
  - `SqlTools` (1 tool) — execute SQL
  - `TagTools` (6 tools) — tag CRUD
  - `DictTools` (14 tools) — dict definitions, items, bindings CRUD
  - `LlmAnalysisTools` (2 tools) — list/get LLM analysis jobs
  - `SchemaDiffTools` (1 tool) — compare crawls
  - `RelationshipTools` (2 tools) — FK traversal, impact analysis
  - `ProfilingTools` (4 tools) — table/column profiling, data sampling, distinct values
  - `AnnotationTools` (3 tools) — update business attributes on schemas, tables, columns
  - `DataQualityTools` (12 tools) — quality rule CRUD, column rule bindings, live quality checks
  - `DatasetTools` (20 tools) — dataset definition CRUD (7), nodes (3), relations (3), filters (2), field mappings (2), execution & instances (3)
- **`repository/`** — Spring Data JPA repositories (29). `MetaTableRepository` and `MetaColumnRepository` also extend `JpaSpecificationExecutor` for advanced search. `spec/` subpackage has `MetaTableSpecifications` and `MetaColumnSpecifications`.
- **`dto/`** — Request/response DTOs (43).

### Database Support

The service can crawl: PostgreSQL, MySQL, Oracle, SQL Server, SQLite. JDBC drivers and SchemaCrawler plugins for all five are included. `JdbcUrlBuilder.buildJdbcUrl()` constructs JDBC URLs from `DataSourceConfig` fields.

### Configuration

Key `application.yml` settings:
- `metadata.crawl.retain-count` (default: 2) — number of successful crawl snapshots to retain per datasource (enables schema diff)
- `metadata.crawl.thread-pool-size` (default: 5) — async crawl thread pool
- `metadata.dataset.thread-pool-size` (default: 3) — dataset execution thread pool
- `metadata.dataset.max-execution-time-seconds` (default: 300) — max dataset execution timeout
- `metadata.dataset.max-rows-per-node` (default: 10000) — max rows per node query
- `metadata.dataset.query-timeout-seconds` (default: 30) — per-query timeout
- `metadata.dataset.max-snapshot-size-bytes` (default: 104857600) — max snapshot size (100MB)
- `metadata.dataset.retain-instance-count` (default: 10) — execution instances retained per dataset
- `metadata.mcp.api-key` (env: `MCP_API_KEY`) — API key for MCP SSE authentication (empty = disabled)

### Conventions

- Lombok (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j`) is used throughout — no manual getters/setters.
- Entities use `GenerationType.IDENTITY` for IDs.
- Metadata queries are always scoped to the latest successful `CrawlJob` for a datasource (via `getLatestCrawlJobId`).
- MCP tools use `@Tool` and `@ToolParam` annotations from Spring AI, registered via `MethodToolCallbackProvider` in `McpServerConfig`.
- MCP tool implementations use `ToolExecutor.run()` / `ToolExecutor.runVoid()` for consistent error handling.
- Error handling uses `ResponseStatusException` for REST and `IllegalArgumentException` for MCP tools.
- No test files exist yet.
