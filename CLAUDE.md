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
```

The service runs on port **9990**. PostgreSQL is used as the metadata store (configured in `src/main/resources/application.yml`). Hibernate `ddl-auto: update` handles schema creation automatically.

## Architecture

This is a Spring Boot 3.4 / Java 21 service that crawls external database schemas using [SchemaCrawler](https://www.schemacrawler.com/) and stores the resulting metadata in PostgreSQL. It exposes both a REST API and an MCP (Model Context Protocol) server for AI tool integration.

### Core Flow

1. **Register a datasource** (`DataSourceConfig`) with connection details
2. **Trigger an async crawl** (`CrawlService.executeCrawl`) — SchemaCrawler connects to the target database, introspects the schema, and the results are persisted as `Meta*` entities
3. **Query metadata** via REST endpoints or MCP tools — always reads from the latest successful crawl job

### Key Packages (`com.rock.metadata`)

- **`model/`** — JPA entities. `DataSourceConfig` stores connection info. `CrawlJob` tracks crawl status/timing. `Meta*` entities (MetaSchema, MetaTable, MetaColumn, MetaPrimaryKey, MetaForeignKey, MetaIndex, MetaTrigger, MetaConstraint, MetaPrivilege, MetaRoutine, MetaSequence) hold crawled metadata.
- **`service/`** — Business logic. `CrawlService` runs async crawls via `@Async` and persists the SchemaCrawler `Catalog`. `MetadataQueryService` reads metadata scoped to the latest successful crawl. `SqlExecuteService` runs arbitrary SQL against registered datasources.
- **`controller/`** — REST endpoints under `/api/`. DataSourceController (CRUD), CrawlController (trigger/status), MetadataController (query), SqlExecuteController (execute SQL).
- **`mcp/`** — Spring AI MCP Server integration. `McpServerConfig` registers tool providers. Tool classes in `mcp/tool/` (DataSourceTools, CrawlTools, MetadataTools, SqlTools) wrap the same services for AI agent consumption. SSE endpoint at `/mcp/message`.
- **`repository/`** — Spring Data JPA repositories, one per entity.
- **`dto/`** — Request/response DTOs.

### Database Support

The service can crawl: PostgreSQL, MySQL, Oracle, SQL Server, SQLite. JDBC drivers and SchemaCrawler plugins for all five are included. The `buildJdbcUrl()` method in `CrawlService` and `SqlExecuteService` constructs JDBC URLs from `DataSourceConfig` fields.

### Conventions

- Lombok (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j`) is used throughout — no manual getters/setters.
- Entities use `GenerationType.IDENTITY` for IDs.
- Metadata queries are always scoped to the latest successful `CrawlJob` for a datasource (via `getLatestCrawlJobId`).
- No test files exist yet.
