package com.this4u.data.engine.command;

import java.util.List;

public record QueryCommand(
    long rootModuleId,
    List<FieldRef> selectFields,
    List<Predicate> filters,   // Predicate: FieldRef op value
    List<OrderBy> orderBy,
    Integer offset,
    Integer limit
) {}
