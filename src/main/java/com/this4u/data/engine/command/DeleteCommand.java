package com.this4u.data.engine.command;

import java.util.List;

public record DeleteCommand(
    long rootModuleId,
    List<Predicate> filters
) {}
