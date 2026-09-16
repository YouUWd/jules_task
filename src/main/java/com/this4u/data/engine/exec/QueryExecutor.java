package com.this4u.data.engine.exec;

import com.this4u.data.engine.plan.BatchLoadNode;
import com.this4u.data.engine.plan.JoinNode;
import com.this4u.data.engine.plan.QueryPlan;
import com.this4u.data.engine.assemble.ResultTreeAssembler;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Component
public class QueryExecutor {

    private final DSLContext dsl;

    public QueryExecutor(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Map<String, Object>> execute(QueryPlan plan) {
        // 1. Main Query: Driver Table + flatJoins (1:1/N:1) one-time LEFT JOIN
        SelectJoinStep<Record> step = dsl.select(plan.selectColumns())
                                          .from(table(plan.getDriverTable()));
        for (JoinNode j : plan.getFlatJoins()) {
            step = step.leftJoin(table(j.getJoinTable()))
                       .on(field(j.getMainField()).eq(field(j.getJoinField())));
        }

        List<Map<String, Object>> rootRows = step.where(plan.conditions())
                            .and(DSL.and(plan.getExistsConditions()))
                            .orderBy(plan.orderByFields())
                            .limit(plan.limit()).offset(plan.offset())
                            .fetchMaps();

        if (rootRows.isEmpty()) {
            return rootRows;
        }

        // 2. Batch load 1:N
        List<Object> rootIds = extractIds(rootRows, "id"); // simplified root driver table id extraction

        for (BatchLoadNode node : plan.getBatchNodes()) {
            var childRows = dsl.selectFrom(table(node.getChildTable()))
                                .where(field(node.getFkColumn()).in(rootIds))
                                .fetchMaps();

            // Recursive load for grandchild 1:N
            if (node.isHasGrandChild()) {
                node.setGrandChildRows(loadGrandChild(node, childRows));
            }
            node.setLoadedRows(groupByFk(childRows, node.getFkColumn()));
        }

        return new ResultTreeAssembler().assemble(rootRows, plan.getBatchNodes());
    }

    // Concrete implementations
    private List<Object> extractIds(List<Map<String, Object>> rows, String idCol) {
        List<Object> ids = new ArrayList<>();
        for(Map<String, Object> row : rows) {
             if (row.containsKey(idCol)) {
                 ids.add(row.get(idCol));
             }
        }
        return ids;
    }

    private Map<Object, List<Map<String, Object>>> loadGrandChild(BatchLoadNode node, List<Map<String, Object>> childRows) {
        if (childRows.isEmpty()) return new HashMap<>();

        List<Object> childIds = extractIds(childRows, "id");
        BatchLoadNode grandChild = node.getGrandChild();

        var grandChildRows = dsl.selectFrom(table(grandChild.getChildTable()))
                                .where(field(grandChild.getFkColumn()).in(childIds))
                                .fetchMaps();

        return groupByFk(grandChildRows, grandChild.getFkColumn());
    }

    private Map<Object, List<Map<String, Object>>> groupByFk(List<Map<String, Object>> rows, String fk) {
        Map<Object, List<Map<String, Object>>> map = new HashMap<>();
        for(Map<String, Object> row : rows) {
             Object fkV = row.get(fk);
             if (fkV != null) {
                  map.computeIfAbsent(fkV, k -> new ArrayList<>()).add(row);
             }
        }
        return map;
    }
}
