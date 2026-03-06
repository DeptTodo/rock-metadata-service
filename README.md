# Rock Metadata Service

A lightweight metadata management service that automatically crawls and catalogs database schemas. Built with Spring Boot and [SchemaCrawler](https://www.schemacrawler.com/).

## Features

- **Multi-database support** — PostgreSQL, MySQL, Oracle, SQL Server, SQLite
- **Async metadata crawling** — register a datasource, trigger a crawl, and query the results via REST API
- **Full schema introspection** — tables, columns, primary keys, foreign keys, indexes
- **Keyword search** — search across table names and column names
- **Crawl job tracking** — status, timing, table/column counts, error messages

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.4.3 |
| SchemaCrawler | 17.7.0 |
| PostgreSQL (metadata store) | 16+ |

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL instance for metadata storage

### 1. Create the metadata database

```sql
CREATE DATABASE rock_metadata;
```

### 2. Configure connection

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rock_metadata
    username: postgres
    password: postgres
```

### 3. Build and run

```bash
mvn clean compile
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

## API Reference

### Datasource Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/datasources` | Register a datasource |
| `GET` | `/api/datasources` | List all datasources |
| `GET` | `/api/datasources/{id}` | Get datasource detail |
| `PUT` | `/api/datasources/{id}` | Update a datasource |
| `DELETE` | `/api/datasources/{id}` | Delete a datasource |

### Crawl Jobs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/datasources/{id}/crawl` | Trigger a metadata crawl |
| `GET` | `/api/crawl-jobs` | List all crawl jobs |
| `GET` | `/api/crawl-jobs/{id}` | Get crawl job status |

### Metadata Query

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/datasources/{id}/schemas` | List schemas |
| `GET` | `/api/datasources/{id}/tables` | List tables (optional `?schema=`) |
| `GET` | `/api/datasources/{id}/search?keyword=` | Search tables & columns |
| `GET` | `/api/tables/{id}` | Full table detail (columns, PKs, FKs, indexes) |
| `GET` | `/api/tables/{id}/columns` | List columns |
| `GET` | `/api/tables/{id}/foreign-keys` | List foreign keys |
| `GET` | `/api/tables/{id}/indexes` | List indexes |

### Example: Register and crawl a PostgreSQL database

```bash
# Register datasource
curl -X POST http://localhost:8080/api/datasources \
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

# Trigger crawl (returns job with id)
curl -X POST http://localhost:8080/api/datasources/1/crawl \
  -H "Content-Type: application/json" \
  -d '{"infoLevel": "detailed"}'

# Check crawl status
curl http://localhost:8080/api/crawl-jobs/1

# Browse metadata
curl http://localhost:8080/api/datasources/1/schemas
curl http://localhost:8080/api/datasources/1/tables
curl http://localhost:8080/api/tables/1
```

## Crawl Info Levels

| Level | Description |
|-------|-------------|
| `minimum` | Table names only |
| `standard` | Tables + columns |
| `detailed` | Tables + columns + keys + indexes |
| `maximum` | Everything including definitions and remarks |

## Project Structure

```
src/main/java/com/rock/metadata/
├── MetadataServiceApplication.java
├── controller/
│   ├── DataSourceController.java
│   ├── CrawlController.java
│   └── MetadataController.java
├── service/
│   ├── CrawlService.java
│   └── MetadataQueryService.java
├── model/
│   ├── DataSourceConfig.java
│   ├── CrawlJob.java, CrawlStatus.java
│   ├── MetaSchema.java, MetaTable.java, MetaColumn.java
│   ├── MetaPrimaryKey.java, MetaForeignKey.java, MetaIndex.java
├── dto/
│   ├── DataSourceRequest.java, CrawlRequest.java
│   ├── TableDetailResponse.java, SearchResult.java
└── repository/
    └── ...Repository.java (Spring Data JPA)
```

## License

[MIT](LICENSE)
