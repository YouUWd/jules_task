package com.this4u.data.engine;

import com.this4u.data.engine.command.FieldRef;
import com.this4u.data.engine.command.InsertCommand;
import com.this4u.data.engine.command.QueryCommand;
import com.this4u.data.engine.exec.InsertExecutor;
import com.this4u.data.engine.exec.QueryExecutor;
import com.this4u.data.engine.meta.MetadataRegistry;
import com.this4u.data.engine.meta.ModuleMeta;
import com.this4u.data.engine.meta.ModuleFieldMeta;
import com.this4u.data.engine.plan.QueryPlan;
import com.this4u.data.engine.plan.QueryPlanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public class VirtualExecutionEngineIntegrationTest {

    @Autowired
    private MetadataRegistry registry;

    @Autowired
    private InsertExecutor insertExecutor;

    @Autowired
    private QueryExecutor queryExecutor;

    @Autowired
    private QueryPlanner queryPlanner;

    @Test
    public void testMetadataLoad() {
        assertNotNull(registry);

        // Verify Module Load
        ModuleMeta studentModule = registry.requireModule(100L);
        assertNotNull(studentModule);
        assertEquals("MOD-STUDENT", studentModule.moduleCode());
        assertEquals("student", studentModule.primaryTable());

        // Verify Virtual Module
        ModuleMeta virtualModule = registry.requireModule(200L);
        assertNotNull(virtualModule);
        assertNull(virtualModule.primaryTable());

        // Verify Physical Anchor Resolution
        String anchor = registry.resolvePhysicalAnchor(virtualModule);
        assertEquals("student", anchor);

        // Verify Fields Loaded
        ModuleFieldMeta fieldMeta = registry.requireField(1001L);
        assertNotNull(fieldMeta);
        assertEquals("id", fieldMeta.columnName());

        fieldMeta = registry.requireField(3203L);
        assertNotNull(fieldMeta);
        assertEquals("title", fieldMeta.columnName());
    }
}
