package com.this4u.data.engine.meta;

public record TableRelationMeta(
    Long id,
    String mainTable,
    String mainField,
    String joinTable,
    String joinField,
    RelationType relationType
) {}
