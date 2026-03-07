package com.rock.metadata.repository.spec;

import com.rock.metadata.model.MetaColumn;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class MetaColumnSpecifications {

    private MetaColumnSpecifications() {}

    public static Specification<MetaColumn> tableIdIn(List<Long> tableIds) {
        return (root, query, cb) -> root.get("tableId").in(tableIds);
    }

    public static Specification<MetaColumn> dataTypeEquals(String dataType) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("dataType")), dataType.toLowerCase());
    }

    public static Specification<MetaColumn> sensitivityLevelEquals(String sensitivityLevel) {
        return (root, query, cb) -> cb.equal(root.get("sensitivityLevel").as(String.class), sensitivityLevel);
    }

    public static Specification<MetaColumn> nullableEquals(boolean nullable) {
        return (root, query, cb) -> cb.equal(root.get("nullable"), nullable);
    }

    public static Specification<MetaColumn> partOfPrimaryKey(boolean value) {
        return (root, query, cb) -> cb.equal(root.get("partOfPrimaryKey"), value);
    }

    public static Specification<MetaColumn> partOfForeignKey(boolean value) {
        return (root, query, cb) -> cb.equal(root.get("partOfForeignKey"), value);
    }

    public static Specification<MetaColumn> tableIdEquals(Long tableId) {
        return (root, query, cb) -> cb.equal(root.get("tableId"), tableId);
    }

    public static Specification<MetaColumn> lastAnalyzedAtIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("lastAnalyzedAt"));
    }

    public static Specification<MetaColumn> columnNameLike(String pattern) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("columnName")),
                "%" + pattern.toLowerCase() + "%");
    }
}
