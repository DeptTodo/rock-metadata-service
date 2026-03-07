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

## Architecture

This is a Spring Boot 3.4 / Java 21 service that crawls external database schemas using [SchemaCrawler](https://www.schemacrawler.com/) and stores the resulting metadata in PostgreSQL. It exposes both a REST API and an MCP (Model Context Protocol) server for AI tool integration.

### Core Flow

1. **Register a datasource** (`DataSourceConfig`) with connection details
2. **Trigger an async crawl** (`CrawlService.executeCrawl`) — SchemaCrawler connects to the target database, introspects the schema, and the results are persisted as `Meta*` entities
3. **Query metadata** via REST endpoints or MCP tools — always reads from the latest successful crawl job
4. **Manage metadata** — tag entities, define data dictionaries, bind dicts to columns
5. **Analyze** — diff schema changes, traverse FK relationships, profile live data

### Key Packages (`com.rock.metadata`)

- **`model/`** — JPA entities. `DataSourceConfig` stores connection info. `CrawlJob` tracks crawl status/timing. `Meta*` entities (MetaSchema, MetaTable, MetaColumn, MetaPrimaryKey, MetaForeignKey, MetaIndex, MetaTrigger, MetaConstraint, MetaPrivilege, MetaRoutine, MetaRoutineColumn, MetaSequence) hold crawled metadata. `MetaTag` for tagging. `DictDefinition`, `DictItem`, `DictColumnBinding` for data dictionaries. `LlmAnalysisJob` for LLM analysis tracking.
- **`service/`** — Business logic:
  - `CrawlService` — async crawls via `@Async`, persists SchemaCrawler `Catalog`, retains last N crawls (configurable via `metadata.crawl.retain-count`)
  - `MetadataQueryService` — reads metadata scoped to latest successful crawl, advanced search with JPA Specifications
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
- **`controller/`** — REST endpoints under `/api/`:
  - `DataSourceController` — datasource CRUD + connection test
  - `CrawlController` — trigger/status crawl jobs
  - `MetadataController` — query metadata, routines, sequences, export, summary, health, diff, relationships, advanced search, profiling
  - `TagController` — tag CRUD (`/api/tags`)
  - `DictController` — dictionary CRUD (`/api/dicts`)
  - `SqlExecuteController` — execute SQL
- **`mcp/`** — Spring AI MCP Server integration. `McpServerConfig` registers 10 tool providers. Tool classes in `mcp/tool/` wrap the same services for AI agent consumption. SSE endpoint at `/mcp/message`.
  - `DataSourceTools` (7 tools) — CRUD + connection test
  - `CrawlTools` (3 tools) — trigger, status, list
  - `MetadataTools` (16 tools) — schemas, tables, columns, FKs, indexes, routines, sequences, search, export, summary, health, advanced search, row counts
  - `SqlTools` (1 tool) — execute SQL
  - `TagTools` (6 tools) — tag CRUD
  - `DictTools` (14 tools) — dict definitions, items, bindings CRUD
  - `LlmAnalysisTools` (2 tools) — list/get LLM analysis jobs
  - `SchemaDiffTools` (1 tool) — compare crawls
  - `RelationshipTools` (2 tools) — FK traversal, impact analysis
  - `ProfilingTools` (2 tools) — table/column profiling
- **`repository/`** — Spring Data JPA repositories. `MetaTableRepository` and `MetaColumnRepository` also extend `JpaSpecificationExecutor` for advanced search. `spec/` subpackage has `MetaTableSpecifications` and `MetaColumnSpecifications`.
- **`dto/`** — Request/response DTOs.

### Database Support

The service can crawl: PostgreSQL, MySQL, Oracle, SQL Server, SQLite. JDBC drivers and SchemaCrawler plugins for all five are included. `JdbcUrlBuilder.buildJdbcUrl()` constructs JDBC URLs from `DataSourceConfig` fields.

### Configuration

Key `application.yml` settings:
- `metadata.crawl.retain-count` (default: 2) — number of successful crawl snapshots to retain per datasource (enables schema diff)
- `metadata.crawl.thread-pool-size` (default: 5) — async crawl thread pool

### Conventions

- Lombok (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j`) is used throughout — no manual getters/setters.
- Entities use `GenerationType.IDENTITY` for IDs.
- Metadata queries are always scoped to the latest successful `CrawlJob` for a datasource (via `getLatestCrawlJobId`).
- MCP tools use `@Tool` and `@ToolParam` annotations from Spring AI, registered via `MethodToolCallbackProvider` in `McpServerConfig`.
- Error handling uses `ResponseStatusException` for REST and `IllegalArgumentException` for MCP tools.
- No test files exist yet.
