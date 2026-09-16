package com.this4u.data.engine.exec;

import com.this4u.data.engine.command.DeleteCommand;
import com.this4u.data.engine.command.Predicate;
import com.this4u.data.engine.meta.MetadataRegistry;
import com.this4u.data.engine.plan.DmlTopology;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Component
public class DeleteExecutor {

    private final DSLContext dsl;
    private final MetadataRegistry registry;

    public DeleteExecutor(DSLContext dsl, MetadataRegistry registry) {
        this.dsl = dsl;
        this.registry = registry;
    }

    @Transactional
    public int execute(DeleteCommand cmd) {
        DmlTopology topo = new DmlTopology(cmd.rootModuleId(), registry);
        List<Object> rootIds = resolveMatchingRootIds(topo.getRootTable(), cmd.filters());

        // Bottom-up: Grandchild -> Child -> 1:1 -> Root
        for (var leaf : topo.getLeafFirstOrder()) {
            dsl.deleteFrom(table(leaf.getTable()))
               .where(field(leaf.getFkColumn()).in(
                   dsl.select(field(leaf.getParentIdColumn()))
                      .from(table(leaf.getParentTable()))
                      .where(field(leaf.getParentTable() + ".id").in(rootIds))
               )).execute();
        }

        return dsl.deleteFrom(table(topo.getRootTable()))
                   .where(field("id").in(rootIds)).execute();
    }

    // Stub
    private List<Object> resolveMatchingRootIds(String table, List<Predicate> filters) {
        return List.of();
    }
}
