---
name: data-prepare
description: >
  Data governance preparation workflow: register a datasource, test connectivity, crawl schema metadata,
  explore and analyze business-relevant tables, and annotate datasource/schemas/tables with business context.
  Use this skill whenever the user wants to onboard a new database, prepare data governance foundations,
  register a datasource for metadata management, crawl and explore a database schema, or annotate
  metadata with business descriptions. Also trigger when the user mentions "data preparation",
  "metadata onboarding", "schema analysis", "data governance setup", or provides database connection
  details (host, port, username, password, database name) and wants to get started.
  Make sure to use this skill even if the user just pastes connection details without explicitly
  asking for "data governance" — that's the most common entry point.
---

# Data Prepare — Data Governance Foundation Workflow

从原始连接信息到全面理解并注解的数据库，建立数据治理的基础。

## Prerequisites

rock-metadata-service 必须运行（端口 9990），MCP 工具可用。工具前缀：`mcp__rock_metadata__`。

## Workflow

```
Phase 1: 连接与注册 → Phase 2: 探索 → Phase 3: 分析 → Phase 4: 注解 → Phase 5: 报告
```

---

### Phase 1: 连接与注册

1. **收集连接信息** — 用户通常一次性提供，只追问缺失的必填项（dbType, host, port, databaseName, username, password）
2. **测试连通性** — `test_connection_adhoc`，确认可达后再注册（避免留下无效数据源记录）
3. **去重检查** — `list_datasources`，按 host+dbType+databaseName 匹配已有记录
   - 若已存在且有成功 crawl → 跳到 Phase 2
   - 若已存在但无 crawl → 跳到第 5 步
4. **注册** — `register_datasource`
5. **爬取** — `trigger_crawl`（infoLevel=maximum），轮询 `get_crawl_job_status` 至完成

### Phase 2: 探索

目标：建立数据库全局画像，为深度分析确定方向。

**⚠ 工具使用要点 — 避免返回数据过大：**
- `list_tables` 默认只返回前 50 张表的详细信息（含 rowCount/comment/domain 等），可通过 `limit` 调整
- `list_table_names` 是轻量级工具，返回逗号分隔的表全名，默认 500 条，支持 offset/limit 分页和 namePattern 过滤
- 大库（500+ 表）时：先用 `list_table_names` 获取全部表名做前缀分组，再用 `list_tables` 针对性获取重点表详情
- **禁止**对大 schema 直接调用 `list_tables(limit=-1)`，会导致返回数据过大

步骤：
1. **Schema 识别** — `list_schemas`，区分应用 Schema 与系统 Schema
2. **表名全景** — 对每个应用 Schema 调用 `list_table_names`（`sortByRowCount=true`）。若 total > 500，分页获取（offset 递增）
3. **前缀分组** — 根据表名前缀（首个 `_` 前的部分）统计各组表数量，识别业务域分布
4. **重点表详情** — `list_tables`（`sortByRowCount=true`，默认 limit=50）获取 Top 表的行数和元信息
5. **向用户呈现概览**：Schema 列表、表前缀分组统计（表名组 + 数量）、Top 20 数据量表
6. **确认分析方向** — 表多（100+）时，与用户确认重点 schema 或业务域再继续

### Phase 3: 分析

目标：理解业务域、实体关系、数据特征。

> 进入此阶段前，先读取 `references/analysis-strategy.md` 获取详细的分层分析方法、噪音过滤信号和域发现策略。

核心原则：
- **分层深入** — 核心表（detail + sample + relationships）→ 次要表（detail only）→ 其余仅识别
- **噪音过滤** — 通过多维信号（命名 + 结构 + 数据量模式）而非硬编码规则判断
- **自下而上发现业务域** — 从表名前缀聚类出发，结合列名和关联关系归纳
- **多维观察** — 结构、数据量、编码体系、敏感字段、关联模式、时间维度

### Phase 4: 注解

目标：将分析知识固化为结构化元数据。描述字段均为 TEXT 类型，无长度限制。

> 进入此阶段前，先读取 `references/annotation-guide.md` 获取各层级的注解写作要求和质量标准。

注解范围（由粗到细）：
1. **数据源** — `update_datasource`：name（业务化命名）+ description（全景描述）
2. **Schema** — `update_schema_attrs`：displayName + businessDescription
3. **核心表** — `update_table_attrs`：displayName + businessDescription + businessDomain + importanceLevel

注解的价值在于让不了解这个数据库的人能快速建立全面理解，同时支持后续治理决策（字典建设、质量规则、影响分析）。因此要尽可能全面详尽，覆盖业务含义、技术特征、数据规模、关联关系等多个维度。

### Phase 5: 报告

> 读取 `references/report-template.md` 获取报告结构模板。

输出结构化分析报告，核心要素：
- 数据源概况 → Schema 架构 → 业务域全景 → 核心表概览
- 技术特征 → 已完成注解清单 → 关键发现 → 后续建议

---

## Error Recovery

- 连通性失败 → 展示具体错误帮助诊断，不注册（避免垃圾数据）
- 爬取失败 → 展示错误，停止分析（基于陈旧数据分析没有价值）
- 分析中单表失败 → 跳过继续（个别失败不影响全局理解）
- 爬取超 5 分钟 → 告知用户，继续等待

## Communication Style

- 中文输出，结构化呈现（表格/列表优于大段文字）
- Phase 2 结束后先展示概览，**等用户确认后**再进入 Phase 3
- 对跳过的内容透明说明（数量 + 原因）
