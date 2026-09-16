package com.this4u.data.engine.meta;

public record ModuleMeta(
    Long id,
    String moduleCode,
    String moduleName,
    String primaryTable,     // null = virtual module
    Long parentId
) {
    public boolean isVirtual() {
        return primaryTable == null;
    }
}
