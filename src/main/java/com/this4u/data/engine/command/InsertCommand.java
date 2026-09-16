package com.this4u.data.engine.command;

import java.util.Map;

// INSERT/UPDATE uses recursive Map structure to hold 1:1(Map) / 1:N(List<Map>)
public record InsertCommand(
    long rootModuleId,
    Map<FieldRef, Object> values
) {}
