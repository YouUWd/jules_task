package com.this4u.data.engine.command;

public record Predicate(
    FieldRef field,
    String op,
    Object value
) {}
