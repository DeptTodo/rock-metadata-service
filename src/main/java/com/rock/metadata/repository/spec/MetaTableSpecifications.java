package com.rock.metadata.repository.spec;

import com.rock.metadata.model.MetaTable;
import org.springframework.data.jpa.domain.Specification;

public final class MetaTableSpecifications {

    private MetaTableSpecifications() {}

    public static Specification<MetaTable> crawlJobIdEquals(Long crawlJobId) {
        return (root, query, cb) -> cb.equal(root.get("crawlJobId"), crawlJobId);
    }

    public static Specification<MetaTable> schemaNameEquals(String schemaName) {
        return (root, query, cb) -> cb.equal(root.get("schemaName"), schemaName);
    }

    public static Specification<MetaTable> tableTypeEquals(String tableType) {
        return (root, query, cb) -> cb.equal(root.get("tableType"), tableType);
    }

    public static Specification<MetaTable> importanceLevelEquals(String importanceLevel) {
        return (root, query, cb) -> cb.equal(root.get("importanceLevel").as(String.class), importanceLevel);
    }

    public static Specification<MetaTable> businessDomainEquals(String businessDomain) {
        return (root, query, cb) -> cb.equal(root.get("businessDomain"), businessDomain);
    }

    public static Specification<MetaTable> tableNameLike(String pattern) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("tableName")),
                "%" + pattern.toLowerCase() + "%");
    }
}
