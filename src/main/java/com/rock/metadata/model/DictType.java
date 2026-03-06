package com.rock.metadata.model;

/**
 * 数据字典结构类型
 */
public enum DictType {

    /** 扁平码表：简单 code -> value 键值对 */
    FLAT,

    /** 树形层级：如行业分类、地区代码，item 之间有 parent-child 关系 */
    TREE,

    /** 枚举类型：固定的有限取值集合 */
    ENUM
}
