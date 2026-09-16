package com.this4u.data.engine.plan;

import com.this4u.data.engine.command.QueryCommand;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QueryPlan {
    private String driverTable;
    private List<JoinNode> flatJoins;
    private List<BatchLoadNode> batchNodes;
    private List<Condition> existsConditions;
    private QueryCommand command;

    // Temporary stub methods for Executor compilation
    public List<Field<?>> selectColumns() {
        return List.of();
    }

    public Condition conditions() {
        return org.jooq.impl.DSL.noCondition();
    }

    public List<SortField<?>> orderByFields() {
        return List.of();
    }

    public int offset() {
        return command.offset() != null ? command.offset() : 0;
    }

    public int limit() {
        return command.limit() != null ? command.limit() : 10;
    }
}
