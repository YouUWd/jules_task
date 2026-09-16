package com.this4u.data.engine.plan;

import com.this4u.data.engine.meta.MetadataRegistry;
import com.this4u.data.engine.meta.ModuleMeta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class DmlTopology {

    @Data
    @NoArgsConstructor
    public static class TopologyNode {
        private String table;
        private Long moduleId;
        private String fkColumn;

        private boolean hasGrandChild;
        private String grandChildKey;
        private TopologyNode grandChild;

        // Fields for deletion reverse topology
        private String parentTable;
        private String parentIdColumn;
    }

    private Long rootModuleId;
    private ModuleMeta rootModule;
    private String rootTable;

    private List<TopologyNode> oneToOneChildren = List.of();
    private List<TopologyNode> oneToManyChildren = List.of();
    private List<TopologyNode> leafFirstOrder = List.of();

    public DmlTopology(long rootModuleId, MetadataRegistry registry) {
        this.rootModuleId = rootModuleId;
        this.rootModule = registry.requireModule(rootModuleId);
        this.rootTable = registry.resolvePhysicalAnchor(this.rootModule);
        // Stub for topology resolution (graph building)
    }
}
