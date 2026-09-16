package com.this4u.data.engine.plan;

import com.this4u.data.engine.command.FieldRef;
import com.this4u.data.engine.command.Predicate;
import com.this4u.data.engine.command.QueryCommand;
import com.this4u.data.engine.meta.MetadataRegistry;
import com.this4u.data.engine.meta.ModuleFieldMeta;
import com.this4u.data.engine.meta.ModuleMeta;
import com.this4u.data.engine.meta.RelationType;
import org.jooq.Condition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QueryPlanner {

    private final MetadataRegistry registry;

    public QueryPlanner(MetadataRegistry registry) {
        this.registry = registry;
    }

    public QueryPlan plan(QueryCommand cmd) {
        ModuleMeta root = registry.requireModule(cmd.rootModuleId());
        String driverTable = registry.resolvePhysicalAnchor(root);

        List<JoinNode> flatJoins = new ArrayList<>();     // 1:1 / N:1 -> LEFT JOIN
        List<BatchLoadNode> batchNodes = new ArrayList<>(); // 1:N -> Secondary batch query

        for (FieldRef ref : cmd.selectFields()) {
            ModuleFieldMeta field = resolveField(ref);
            if (field == null) continue;

            ModuleMeta owningModule = registry.requireModule(field.moduleId());
            if (owningModule == null || owningModule.primaryTable() == null) {
                continue; // Virtual module itself has no fields
            }

            RelationType rel = registry.relationBetween(driverTable, owningModule.primaryTable());
            if (rel == null) continue;

            switch (rel) {
                case ONE_TO_ONE:
                case MANY_TO_ONE:
                    flatJoins.add(buildJoinNode(driverTable, owningModule));
                    break;
                case ONE_TO_MANY:
                    batchNodes.add(buildBatchNode(driverTable, owningModule, cmd));
                    break;
            }
        }

        // If filters reference 1:N fields (like skill_level in Scene B),
        // Compile to EXISTS subquery instead of JOIN to prevent cartesian product.
        List<Condition> existsConditions = compileOneToManyFiltersAsExists(cmd.filters(), driverTable);

        return new QueryPlan(driverTable, flatJoins, batchNodes, existsConditions, cmd);
    }

    // Stub implementations for the planner
    private ModuleFieldMeta resolveField(FieldRef ref) {
        if (ref.fieldId() != null) {
            return registry.requireField(ref.fieldId());
        }
        return null;
    }

    private JoinNode buildJoinNode(String driverTable, ModuleMeta target) {
        return new JoinNode(driverTable, "id", target.primaryTable(), "target_id");
    }

    private BatchLoadNode buildBatchNode(String driverTable, ModuleMeta target, QueryCommand cmd) {
        return new BatchLoadNode(driverTable, target.primaryTable(), "driver_id", target.id());
    }

    private List<Condition> compileOneToManyFiltersAsExists(List<Predicate> filters, String driverTable) {
        return new ArrayList<>();
    }
}
