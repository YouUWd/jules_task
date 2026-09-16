package com.this4u.data.engine.meta;

public record ModuleFieldMeta(
    Long id,
    Long moduleId,
    String tableName,
    String columnName,
    String displayName
) {}
