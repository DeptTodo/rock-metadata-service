---
name: data-dict
description: >
  Data dictionary discovery and migration workflow: scan a registered datasource's metadata to identify
  dictionary/lookup tables, extract their code-value pairs, and migrate them into the rock-metadata
  data dictionary service. Use this skill whenever the user wants to discover dictionary tables in a
  database, import lookup data into the metadata service, build a data dictionary from existing database
  tables, or migrate reference/code tables. Also trigger when the user mentions "data dictionary",
  "lookup tables", "code tables", "reference data migration", "dict migration", or asks to find and
  import enum/code/type tables from a datasource that has already been crawled.
---

# Data Dict - Dictionary Table Discovery & Migration

This skill discovers dictionary/lookup tables in a crawled datasource, extracts their code-value data, and migrates it into the rock-metadata data dictionary service. It bridges the gap between raw database reference tables and structured, reusable data dictionaries.

## Prerequisites

- The rock-metadata-service must be running (port 9990) with MCP tools available
- The target datasource must already be registered and have a successful crawl (typically via the `data-prepare` skill)
- MCP tool prefix: `mcp__rock_metadata__`

## Workflow Overview

1. Confirm the target datasource and review existing metadata
2. Discover dictionary table candidates using name patterns + structural analysis
3. Present candidates to the user for confirmation
4. Extract data from confirmed dictionary tables
5. Create dict definitions and import items into the metadata service
6. Discover and bind dictionaries to business table columns
7. Report migration results

## Step 1: Confirm Target Datasource

The user will reference a datasource — by ID, name, or just context from a previous data-prepare session.

Call `list_datasources` to find the target. If ambiguous, ask the user to clarify.

Then call `list_schemas` for that datasource to understand the schema layout. Note which schemas are application schemas vs system schemas — dictionary tables are almost always in application schemas.

Also call `list_dicts` with the datasource ID to check what dictionaries already exist. This prevents duplicate migration. If dicts already exist, inform the user and ask whether to skip existing ones or re-import.

## Step 2: Discover Dictionary Table Candidates

This is the core intelligence of the skill. Dictionary tables share common characteristics across different systems, but naming conventions vary widely. Use a multi-signal approach.

### 2a. List All Tables

Call `list_tables` for each application schema. You'll need the full table list to scan for candidates.

### 2b. Name Pattern Matching

Scan table names against known dictionary table patterns. Different systems use different conventions — cast a wide net and score by confidence.

**High-confidence patterns** (very likely to be dictionary tables):
- Contains `dict` or `dictionary`: `sys_dict`, `t_dict`, `frame_dictionary`, `data_dict`
- Contains `code` as a component: `sys_code`, `code_table`, `frame_code`, `t_code`
- Contains `lookup` or `lut`: `lookup_status`, `lut_category`
- Exact pattern `*_enum` or `enum_*`

**Medium-confidence patterns** (likely, but need structural verification):
- Contains `type` as suffix with a domain prefix: `project_type`, `user_type`, `order_type`
- Contains `status` as suffix: `order_status`, `audit_status`
- Contains `category` or `classify`: `product_category`, `classify_info`
- Contains `level` or `grade`: `risk_level`, `member_grade`
- Contains `ref_` prefix: `ref_country`, `ref_currency`
- Contains `config` or `param`: `sys_config`, `sys_param` (sometimes key-value dicts)

**Low-confidence patterns** (possible, depends on structure):
- Contains `info` with a short qualifier: `area_info`, `dept_info`
- Standalone domain words: `region`, `district`, `department`

**System-specific patterns to watch for** (common in Chinese government/enterprise systems):
- ePoint bidding: `frame_code*`, `sys_*` with few columns, `*fenlei`, `*leibie`, `*leixing`
- Generic Chinese: tables with `字典`, `编码`, `类型`, `分类` in remarks/comments

### 2c. Structural Analysis

For tables matching name patterns, verify they have dictionary-like structure by calling `get_table_detail`. A dictionary table typically has:

**Strong structural signals:**
- **Few columns** (2-6 columns). Core dictionary tables have a code column, a value/label column, and maybe sort order, parent ID, description
- **Small row count** (< 5000 rows). Most dictionaries have dozens to hundreds of items, rarely thousands
- **Column names** that suggest code-value pairs: columns named `code`/`value`, `key`/`name`, `id`/`name`, `bm`/`mc` (编码/名称), `dm`/`mc` (代码/名称)
- **No foreign keys pointing outward** — dictionary tables are referenced by others, they rarely reference other tables
- **No timestamp columns** or very few (maybe just created/updated) — they're not transactional

**Disqualifying signals:**
- Many columns (> 10) — probably a business entity table, not a dictionary
- Very high row count (> 10000) — probably transactional data
- Multiple foreign keys — probably a relationship/junction table
- Contains LOB/text columns — probably storing documents, not codes

### 2d. Scoring and Ranking

Assign each candidate a confidence score:
- **Name match score**: high pattern = 3, medium = 2, low = 1
- **Structure score**: 2-6 columns = 3, 7-10 columns = 1, >10 = -2
- **Row count score**: <100 = 3, 100-1000 = 2, 1000-5000 = 1, >5000 = -1
- **Column name score**: has code/value pair columns = 2, unclear = 0

Total score > 5 = high confidence, 3-5 = medium, < 3 = low.

### 2e. Cross-Schema Deduplication

When multiple schemas exist, the same logical dictionary table may appear in more than one schema (e.g., `schema_a.sys_code` and `schema_b.sys_code`). Compare:
- Table name match
- Column structure similarity
- If identical, prefer the schema with more data (higher row count)
- Flag duplicates to the user: "该字典表在多个 Schema 中存在，建议选择数据更完整的版本"

## Step 3: Present Candidates for Confirmation

Present the discovered candidates to the user in a structured table, grouped by confidence:

```
## 发现的字典表候选

### 高置信度 (建议全部导入)
| Schema | 表名 | 行数 | 列数 | 疑似编码列 | 疑似名称列 | 说明 |
|--------|------|------|------|-----------|-----------|------|
| ...    | ...  | ...  | ...  | ...       | ...       | ...  |

### 中置信度 (建议逐个确认)
| Schema | 表名 | 行数 | 列数 | 疑似编码列 | 疑似名称列 | 说明 |
| ...    | ...  | ...  | ...  | ...       | ...       | ...  |

### 低置信度 (可能不是字典表)
| ...    | ...  | ...  | ...  | ...       | ...       | ...  |

### 跨 Schema 重复
- `schema_a.sys_code` 与 `schema_b.sys_code` 结构相同，建议使用 schema_b（数据更完整）
```

Ask the user: "请确认要导入哪些表？可以说'全部高置信度'、'排除 xxx'、或逐个指定。"

Wait for user confirmation before proceeding. Do not import anything without explicit approval.

## Step 4: Extract Dictionary Data

For each confirmed dictionary table:

### 4a. Identify Code and Value Columns

Call `get_table_detail` (if not already done) and analyze columns to determine:
- **Code column**: the stored value column (PK, or column named `*code`, `*bm`, `*dm`, `*id`, `*key`, `*no`)
- **Value column**: the display label column (column named `*name`, `*mc`, `*value`, `*label`, `*text`, `*title`, `*description`)
- **Sort column**: optional (column named `*sort*`, `*order*`, `*seq*`, `*px*`)
- **Parent column**: for tree dicts (column named `*parent*`, `*pid*`, `*fid*`, `*sjbm*`)
- **Active/status column**: optional (column named `*status*`, `*active*`, `*enabled*`, `*flag*`)

If code/value columns can't be determined automatically, sample a few rows with `sample_table_rows` and infer from the data. If still unclear, ask the user.

### 4b. Extract Data

For small tables (< 100 rows), use `sample_table_rows` with `limit: 100`.

For larger tables (100-5000 rows), use `execute_sql` with a targeted SELECT:
```sql
SELECT code_col, value_col, sort_col FROM schema.table ORDER BY sort_col
```

### 4c. Handle Tree-Structured Dicts

If a parent column is detected, the dict is tree-structured:
- Set `dictType: "TREE"` when creating the definition
- Import items level by level: first root items (parent IS NULL), then children
- Set `treeLevel` based on depth (0 for root, 1 for children, etc.)
- Set `parentId` by matching the parent code to the already-imported parent item's ID

## Step 5: Create Dicts and Import Items

### 5a. Generate Dict Code

The `dictCode` must be globally unique. Use this convention:

**When the datasource has a single application schema:**
- `dictCode` = table name in UPPER_SNAKE_CASE (e.g., `sys_code` → `SYS_CODE`)

**When the datasource has multiple application schemas:**
- `dictCode` = `{SCHEMA_SHORT}_{TABLE}` in UPPER_SNAKE_CASE
- Schema short name: strip common prefixes to keep codes manageable
  - e.g., `epointbid_pb_yiyang.frame_code` → `PB_FRAME_CODE`
  - e.g., `epointbid_tp7_yy.sys_dict` → `TP7_SYS_DICT`
- When presenting to the user, explain the naming convention and let them adjust

**Always check** `list_dicts` or attempt `get_dict_by_code` before creating — skip if a dict with the same code already exists, and inform the user.

### 5b. Create Dict Definition

For each confirmed table, call `create_dict`:
- `dictCode`: generated per convention above
- `dictName`: derive from table name or table comment (prefer Chinese name if available from metadata remarks)
- `dictType`: `"FLAT"` for simple code-value tables, `"TREE"` if parent column detected, `"ENUM"` for very small fixed sets (< 10 items, no sort column)
- `sourceType`: `"CRAWLED"` (always — these are discovered from crawled metadata)
- `datasourceId`: the target datasource ID
- `sourceSchemaName`: the schema where the table lives
- `sourceTableName`: the original table name
- `description`: brief description of what this dictionary contains

### 5c. Import Dict Items

For each extracted row, call `add_dict_item`:
- `dictId`: from the created dict definition
- `itemCode`: value from the code column (convert to string)
- `itemValue`: value from the value column
- `sortOrder`: from sort column if available, otherwise use row order
- `treeLevel`: for tree dicts, based on hierarchy depth
- `parentId`: for tree dicts, the ID of the parent item (looked up after import)

**Batching strategy**: Import items in batches. For each dict table:
1. Create the definition first
2. Import all items sequentially (the MCP API is item-by-item)
3. For tree dicts, import root items first, then recursively import children

**Error handling**: If an individual item fails, log it and continue with the rest. Report failures at the end.

### 5d. Progress Reporting

For large migrations (> 10 tables), report progress after every 3-5 tables:
"已完成 5/15 个字典表导入（共导入 342 个字典项）"

## Step 6: Discover and Bind Dictionaries to Columns

After importing dictionaries, the next high-value step is binding them to the business table columns that actually use these code values. This creates a live mapping between "where the code is defined" and "where the code is used."

### 6a. Column Discovery Strategy

For each imported dictionary, find columns in business tables that store values from that dictionary. Use three complementary approaches:

**Approach 1: Column Name Matching**

Compare business table column names against the dictionary's source table name or dict code. Common patterns:
- Dict from `project_type` table → look for columns named `projecttype`, `project_type`, `projectlx`, `xmlx`
- Dict from `area_code` table → look for columns named `areacode`, `area_code`, `xiaqucode`, `belongxiaqucode`
- Dict from `zhaobiaofangshi` table → columns named `zhaobiaofangshi`, `zbfangshi`, `zbfs`

Use `advanced_search` or `list_columns` to search for columns whose names contain the dict's key terms. Cast a wide net — column naming is inconsistent across tables.

**Approach 2: Value Matching (Sampling)**

For high-value dictionaries (those likely used across many tables), use `get_distinct_column_values` on suspect columns and compare with dict item codes:
- If > 80% of a column's distinct values exist in the dict's item codes → strong match
- If > 50% → possible match, present to user
- If < 50% → probably not a match

This is the most reliable approach but also the most expensive (requires live database queries). Use selectively — prioritize columns identified by name matching first.

**Approach 3: Comment/Remark Matching**

If table or column comments/remarks mention the dictionary name or related terms, that's a strong signal. Check column comments from `get_table_detail` results.

### 6b. Present Binding Candidates

After discovery, present candidates to the user:

```
## 字典-列绑定建议

### 字典: 招标方式 (ZHAOBIAO_FANGSHI)
| Schema | 表名 | 列名 | 匹配方式 | 置信度 |
|--------|------|------|----------|--------|
| epointbid_tp7_yy | cg_projectinfo | zhaobiaofangshi | 列名匹配 | 高 |
| epointbid_tp7_yy | jsgc_biaoduaninfo | zhaobiaofangshi | 列名匹配 | 高 |
| epointbid_pb_yiyang | pingbiao_kaibiaotoubiao | dblx | 值匹配 | 中 |

### 字典: 行政区划 (AREA_CODE)
| Schema | 表名 | 列名 | 匹配方式 | 置信度 |
|--------|------|------|----------|--------|
| epointbid_tp7_yy | cg_projectinfo | xiaqucode | 列名匹配 | 高 |
| epointbid_tp7_yy | cg_projectinfo | belongxiaqucode | 列名匹配 | 高 |
| ... | ... | ... | ... | ... |
```

Ask the user to confirm which bindings to create. High-confidence bindings can be batch-approved.

### 6c. Create Bindings

For each confirmed binding, call `bind_dict_to_column`:
- `dictId`: the dictionary's ID
- `datasourceId`: the target datasource ID
- `schemaName`: the schema of the business table
- `tableName`: the business table name
- `columnName`: the column name
- `metaColumnId`: if available from metadata (look up via `list_columns` or `get_table_detail`)
- `bindingType`:
  - `"NAME_MATCH"` for Approach 1 matches
  - `"LLM_INFERRED"` for Approach 2/3 matches or when the model infers the relationship
  - `"MANUAL"` only if the user explicitly specifies a binding
- `confidence`: 0.0-1.0 based on match quality (name exact match = 0.9, partial = 0.7, value match = 0.8, inference = 0.5)

### 6d. Scope Control

Column binding discovery can be very broad in large databases. Apply these limits:
- Focus on **Tier 1 business tables** (high row count, structurally important) from the data-prepare analysis
- Limit to the top 20-30 most important business tables per schema
- For each dictionary, search up to 50 columns — don't scan every column in every table
- If the database has > 500 tables, ask the user which schemas or table groups to focus on

### 6e. Avoid False Positives

Some column names are ambiguous. Be cautious with:
- Generic names like `type`, `status`, `code` — these exist in many tables with different meanings
- Columns that share a dict name but store different semantics (e.g., a `status` column might be "project status" not "audit status")
- When in doubt, sample a few values with `get_distinct_column_values` and compare with dict items before suggesting

## Step 7: Deliver Migration Report

After all confirmed tables are processed, present a structured report:

```
## 数据字典迁移报告

### 迁移概况
- 数据源: {datasource name} (ID: {id})
- 扫描表数: {total tables scanned}
- 发现候选: {candidates found}
- 确认导入: {confirmed by user}
- 成功导入: {successful}
- 导入失败: {failed}

### 导入明细
| 字典编码 | 字典名称 | 来源表 | 类型 | 字典项数 | 状态 |
|----------|----------|--------|------|---------|------|
| PB_FRAME_CODE | 框架编码 | epointbid_pb_yiyang.frame_code | FLAT | 45 | 成功 |
| ... | ... | ... | ... | ... | ... |

### 失败项 (如有)
| 字典编码 | 来源表 | 错误原因 |
|----------|--------|----------|
| ... | ... | ... |

### 列绑定结果
| 字典编码 | 字典名称 | 绑定列 | 匹配方式 | 置信度 |
|----------|----------|--------|----------|--------|
| AREA_CODE | 行政区划 | cg_projectinfo.xiaqucode | NAME_MATCH | 0.9 |
| ... | ... | ... | ... | ... |

- 共绑定 {N} 个列到 {M} 个字典
- 跳过 {K} 个低置信度匹配

### 后续建议
- 发现 {N} 个疑似编码列尚未覆盖，可手动创建字典
- 部分通用编码（如性别、是否）可手动创建后批量绑定
- 可使用 `bind_dict_to_column` 手动补充绑定关系
```

## Communication Style

- Present all findings and reports in Chinese
- Use tables for structured data — easier to scan than paragraphs
- Be transparent about confidence levels: don't auto-import low-confidence candidates
- Always wait for user confirmation before creating/importing anything
- Report progress for large migrations

## Error Recovery

- If `list_tables` fails for a schema, skip that schema and continue with others
- If `get_table_detail` fails for a candidate, remove it from the candidate list
- If `create_dict` fails (e.g., duplicate code), inform the user and skip
- If `add_dict_item` fails for individual items, continue importing the rest and report failures
- If `execute_sql` fails (permission denied), fall back to `sample_table_rows`
- Never leave a half-imported dict — if too many items fail (> 50%), offer to delete the dict definition and retry

## Edge Cases

### Very Large Dictionary Tables (> 1000 items)
Some tables that look like dictionaries may actually be master data (e.g., a `region` table with 3000+ administrative divisions). These are still useful as dictionaries. Import them but warn the user about the size. Use `execute_sql` with pagination if needed.

### Multi-Column Code Tables
Some dictionary tables use composite keys (e.g., `type_code` + `sub_code`). For these:
- Use concatenated code: `{type_code}:{sub_code}` as itemCode
- Or suggest the user split into separate dictionaries per type_code
- Ask the user which approach they prefer

### Tables with Active/Status Flags
If a dictionary table has a status column (e.g., `is_active`, `status`), only import active/enabled rows by default. Inform the user: "该表包含已停用的字典项，默认仅导入有效项。需要导入全部吗？"

### Character Encoding
Some legacy databases store Chinese values in non-UTF8 encodings. If sampled values appear garbled, warn the user about potential encoding issues.
