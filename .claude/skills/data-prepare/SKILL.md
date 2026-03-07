---
name: data-prepare
description: >
  Data governance preparation workflow: register a datasource, test connectivity, crawl schema metadata,
  explore and analyze business-relevant tables, and annotate datasource/schemas with business context.
  Use this skill whenever the user wants to onboard a new database, prepare data governance foundations,
  register a datasource for metadata management, crawl and explore a database schema, or annotate
  metadata with business descriptions. Also trigger when the user mentions "data preparation",
  "metadata onboarding", "schema analysis", "data governance setup", or provides database connection
  details (host, port, username, password, database name) and wants to get started.
---

# Data Prepare - Data Governance Foundation Workflow

This skill automates the end-to-end data governance preparation process using the rock-metadata-service MCP tools. It takes a user from raw connection details to a well-understood, annotated database — establishing the foundation for subsequent governance work (table annotation, data dictionary, quality rules, etc.).

## Prerequisites

The rock-metadata-service must be running (port 9990) with MCP tools available. The MCP server name is `rock-metadata` — all tool calls use the `mcp__rock_metadata__` prefix.

## Workflow Overview

1. Gather connection info from the user
2. Test connectivity (before registration)
3. Check for existing datasource (avoid duplicates)
4. Register datasource if new
5. Trigger schema crawl and wait for completion (or reuse existing crawl)
6. Explore the database: summary, schemas, row counts
7. Analyze business-relevant tables (skip noise tables, cap depth)
8. Annotate datasource and schemas with business metadata
9. Deliver structured analysis report

## Step 1: Gather Connection Info

Users typically provide connection info in one message — accept whatever they give and only ask for missing **required** fields. Don't interrogate field by field.

Required fields:
- **dbType**: postgresql, mysql, oracle, sqlserver, or sqlite
- **host** + **port** + **databaseName** (or **jdbcUrl** as an alternative)
- **username** and **password**

Optional fields (fill in sensible defaults if not given):
- **name**: display name — default to `{host}/{databaseName}` if not provided
- **schemaPatterns**: regex to include specific schemas — suggest if the database is known to have many schemas
- **description**: can be left empty; will be enriched in Step 8

## Step 2: Test Connectivity First

Before registering anything, call `test_connection_adhoc` with the user's connection parameters to verify the database is reachable.

- **If success**: Report database product info and response time, then proceed to registration.
- **If failed**: Show error details and help diagnose. Common issues:
  - Wrong credentials → ask user to verify username/password
  - Host unreachable → check host/port, firewall, VPN
  - Database not found → verify databaseName spelling
  - Do NOT proceed until connectivity is confirmed. No point registering a broken datasource.

## Step 3: Check for Existing Datasource

Call `list_datasources` and check if a datasource with matching connection info already exists. Match by:
- `host` + `dbType` + `databaseName` (case-insensitive), OR
- `jdbcUrl` if the user provided one

**If found**:
- Tell the user: "该数据源已注册（ID: X, 名称: Y），可以直接使用"
- Check if it already has a successful crawl by calling `list_crawl_jobs` for that datasource ID
  - **Has successful crawl**: Skip to Step 6 (exploration) — no need to re-crawl
  - **No successful crawl**: Skip to Step 5 (trigger crawl)
- Always ask the user to confirm before reusing

**If not found**: Proceed to Step 4.

## Step 4: Register Datasource

Call `register_datasource` with the gathered parameters. Note the returned datasource ID for all subsequent steps.

## Step 5: Trigger Crawl and Wait

First, check if the datasource already has a recent successful crawl (from Step 3). If so, skip this step.

Call `trigger_crawl` with the datasource ID and `infoLevel: "maximum"`.

The crawl is asynchronous. Polling strategy:
1. Note the returned crawl job ID
2. Wait ~5 seconds, then call `get_crawl_job_status`
3. If still PENDING or RUNNING, wait ~10 seconds and poll again
4. Continue polling with 10-second intervals
5. If status is `SUCCESS`: report crawl stats and proceed
6. If status is `FAILED`: show the error to the user and stop
7. If still running after 5 minutes: inform the user it's taking longer than usual, continue waiting but suggest checking if the target database is responsive

## Step 6: Explore the Database

Build a high-level understanding before diving into details.

### 6a. Get Datasource Summary

Call `get_datasource_summary` to get total counts, table type distribution, column type distribution, and tables with the most columns/indexes. Present a brief overview to the user.

### 6b. List Schemas

Call `list_schemas` to see all available schemas. Identify application schemas vs system schemas (e.g., `information_schema`, `pg_catalog` are system schemas in PostgreSQL).

### 6c. Count Table Rows

Call `count_table_rows` **filtered by schema** — do one application schema at a time rather than the entire database. For databases with a single application schema, one call suffices.

Present the top 20 tables by row count to show data volume distribution. This informs which tables deserve deeper analysis.

## Step 7: Analyze Business-Relevant Tables

The goal: understand what the database contains, how the core entities relate, and what business domains are represented. This understanding feeds into the annotations in Step 8.

### Filtering: Business Tables vs Noise Tables

**SKIP** infrastructure/operational tables — names containing:
- Log/audit: `log`, `audit`, `trace`, `changelog`, `history_log`
- Event/message: `event_queue`, `message_queue`, `notification`, `mq_`, `job_queue`
- Auth/permission: `permission`, `role_permission`, `oauth_`, `token`, `session`
- System/framework: `sys_`, `system_`, `flyway`, `liquibase`, `databasechangelog`, `__migration`
- Scheduling: `quartz_`, `qrtz_`, `scheduled_`, `cron_`
- Temp/cache: `tmp_`, `temp_`, `cache_`
- High row count + simple structure (e.g., millions of rows, 3-5 columns like timestamp/level/message)

**FOCUS** on: entity tables, transaction tables, reference tables, and relationship/junction tables.

Use judgment — naming conventions vary across projects. Look at column count, foreign keys, and row count patterns, not just table names.

### Analysis Strategy: Tiered Depth

Don't analyze every business table in equal depth. Use a tiered approach based on row count and structural complexity:

**Tier 1 — Deep analysis (top 5-10 tables by row count + FK richness)**:
1. `get_table_detail` — columns, PKs, FKs, indexes
2. `sample_table_rows` (5 rows) — see real data patterns
3. `get_table_relationships` (depth 1) — understand connections

**Tier 2 — Light analysis (next 10-15 tables)**:
1. `get_table_detail` only — understand structure without sampling

**Tier 3 — Acknowledge only (remaining business tables)**:
- Note their existence and apparent purpose from name/column names, but don't call individual tools

For databases with 100+ tables, ask the user which schemas or business areas to focus on before starting analysis.

### Building Understanding

As you analyze, build a picture of:
- **Business domains**: Group tables by area (用户域, 订单域, 商品域, 财务域, etc.)
- **Entity relationships**: How core entities connect
- **Data characteristics**: Volume distribution, reference vs transactional data
- **Notable findings**: Unusual patterns, potential data quality concerns, columns that look like enum/code candidates (useful for future dictionary work)

## Step 8: Annotate Metadata

Based on the analysis, write annotations at the datasource and schema level.

### 8a. Annotate Datasource

Call `update_datasource` to update:
- **name**: Only update if the current name is generic or auto-generated. If the user deliberately chose a name, keep it.
- **description**: A comprehensive business-level summary:
  - Business system this database belongs to
  - Business domains covered (e.g., "包含用户、订单、商品、支付四大业务域")
  - Data scale (e.g., "共 45 张业务表，核心表数据量在百万级")
  - Key characteristics discovered during analysis

### 8b. Annotate Schemas

For each application schema, call `update_schema_attrs` with:
- **displayName**: Clear Chinese business name (e.g., "核心业务库", "数据仓库")
- **businessDescription**: What this schema contains, its purpose, and a brief summary of the major business domains and core tables within it
- **owner**: Leave blank unless the user specifies

Table-level and column-level annotation is NOT part of this stage. The exploration in Step 7 is for building understanding to write good datasource and schema annotations. Detailed table/column governance belongs to subsequent workflows.

## Step 9: Deliver Analysis Report

After completing all annotations, present a structured summary report to the user. Use this template:

```
## 数据治理准备报告

### 数据源概况
- 名称: {datasource name}
- 类型: {dbType}
- 数据库: {host}/{databaseName}
- Schema 数量: {N}
- 表总数: {N} (业务表 {N} / 系统表 {N})

### 业务域分布
| 业务域 | 核心表 | 说明 |
|--------|--------|------|
| 用户域 | user, user_profile, ... | 用户注册、资料管理 |
| ...    | ...                     | ...                |

### 核心表概览 (按数据量排序)
| 表名 | 行数 | 列数 | 外键数 | 简要说明 |
|------|------|------|--------|----------|
| ...  | ...  | ...  | ...    | ...      |

### 关键发现
- {notable patterns, data quality observations, relationship highlights}

### 跳过的表 ({N} 张)
- 日志类: {list}
- 系统类: {list}
- ...

### 后续建议
- 建议优先对 {domain} 域的核心表进行详细注解
- 发现 {N} 个疑似枚举/编码列，建议建立数据字典
- ...
```

This report gives the user a clear picture of what was done and what to do next.

## Communication Style

- Present findings in Chinese with clear structure
- Use tables or bullet lists for summaries — avoid walls of text
- After Step 6 (exploration), briefly share the overview and confirm before spending time on deep analysis in Step 7
- Be transparent about what you're skipping and why (e.g., "跳过了 15 张日志/系统表，聚焦分析 23 张业务核心表")

## Error Recovery

- If `test_connection_adhoc` fails, help diagnose — do NOT register the datasource
- If a crawl fails, show the error and stop — don't analyze stale/missing data
- If a specific tool call fails mid-analysis, skip that table and continue with others
- If the crawl takes > 5 minutes, inform the user but keep waiting
- If datasource was registered but crawl keeps failing, suggest checking the connection or schema patterns
