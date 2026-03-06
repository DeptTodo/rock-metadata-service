package com.rock.metadata.model;

/**
 * 数据字典来源类型
 */
public enum DictSourceType {

    /** 从数据库表自动抓取（如 crawl 发现的码表） */
    CRAWLED,

    /** 手工创建 */
    MANUAL,

    /** 从外部文件/系统导入（Excel、CSV、JSON、其他元数据平台等） */
    IMPORTED
}
