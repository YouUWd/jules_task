package com.this4u.data.engine.exec;

import com.this4u.data.engine.command.InsertCommand;
import com.this4u.data.engine.command.FieldRef;
import com.this4u.data.engine.meta.MetadataRegistry;
import com.this4u.data.engine.meta.ModuleMeta;
import com.this4u.data.engine.plan.DmlTopology;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Component
public class InsertExecutor {

    private final DSLContext dsl;
    private final MetadataRegistry registry;

    public InsertExecutor(DSLContext dsl, MetadataRegistry registry) {
        this.dsl = dsl;
        this.registry = registry;
    }

    @Transactional
    public Long execute(InsertCommand cmd) {
        DmlTopology topo = new DmlTopology(cmd.rootModuleId(), registry); // Root -> 1:1 -> 1:N -> Grandchild

        // 1. Root table insertion
        Long rootId = insertScalarFields(topo.getRootTable(), scalarsOf(cmd, topo.getRootModule()));

        // 2. 1:1 Sub tables: Backfill FK and insert
        for (var one2one : topo.getOneToOneChildren()) {
            Map<String, Object> obj = (Map<String, Object>) resolveValue(cmd, one2one.getModuleId());
            if (obj != null) {
                obj.put(one2one.getFkColumn(), rootId);
                insertScalarFields(one2one.getTable(), obj);
            }
        }

        // 3. 1:N Child tables: Row by row, backfill FK to grandchild
        for (var one2many : topo.getOneToManyChildren()) {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) resolveValue(cmd, one2many.getModuleId());
            if (rows == null) continue;

            for (Map<String, Object> row : rows) {
                Object grandChildPayload = null;
                if (one2many.getGrandChildKey() != null) {
                    grandChildPayload = row.remove(one2many.getGrandChildKey());
                }
                row.put(one2many.getFkColumn(), rootId);
                Long childId = insertScalarFields(one2many.getTable(), row);

                if (grandChildPayload != null) {
                    insertGrandChildren(one2many.getGrandChild(), childId, (List<Map<String, Object>>) grandChildPayload);
                }
            }
        }
        return rootId;
    }

    // Complete implementations
    private Long insertScalarFields(String tableName, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        Map<org.jooq.Field<Object>, Object> jooqValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            jooqValues.put(field(entry.getKey()), entry.getValue());
        }

        return dsl.insertInto(table(tableName))
           .set(jooqValues)
           .returning(field("id", Long.class))
           .fetchOne()
           .getValue(field("id", Long.class));
    }

    private void insertGrandChildren(DmlTopology.TopologyNode node, Long parentId, List<Map<String, Object>> rows) {
        if (node == null || rows == null || rows.isEmpty()) return;

        for(Map<String, Object> row : rows) {
             row.put(node.getFkColumn(), parentId);
             insertScalarFields(node.getTable(), row);
        }
    }

    private Map<String, Object> scalarsOf(InsertCommand cmd, ModuleMeta module) {
        Map<String, Object> scalarValues = new HashMap<>();
        if (cmd.values() != null) {
             for(Map.Entry<FieldRef, Object> entry : cmd.values().entrySet()) {
                 Object val = entry.getValue();
                 if (val != null && !(val instanceof Map) && !(val instanceof List)) {
                     // Need column name from registry in a real implementation
                     // Using fieldId as fallback for now
                     scalarValues.put(String.valueOf(entry.getKey().fieldId()), val);
                 }
             }
        }
        return scalarValues;
    }

    private Object resolveValue(InsertCommand cmd, Long moduleId) {
        if (cmd.values() != null) {
             for(Map.Entry<FieldRef, Object> entry : cmd.values().entrySet()) {
                 if (entry.getKey().fieldId() != null && entry.getKey().fieldId().equals(moduleId)) {
                      return entry.getValue();
                 }
             }
        }
        return null;
    }
}
