package com.this4u.data.engine.command;

public record FieldRef(Long fieldId, String modulePath) {
    public static FieldRef byId(long id) {
        return new FieldRef(id, null);
    }

    public static FieldRef byPath(String path) {
        return new FieldRef(null, path);
    }
}
