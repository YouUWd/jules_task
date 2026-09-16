package com.this4u.data.engine.command;

import java.util.List;
import java.util.Map;

public record UpdateCommand(
    long rootModuleId,
    Map<FieldRef, Object> setValues,
    List<Predicate> filters
) {}
