package com.this4u.data.engine.command;

public record OrderBy(
    FieldRef field,
    boolean ascending
) {}
