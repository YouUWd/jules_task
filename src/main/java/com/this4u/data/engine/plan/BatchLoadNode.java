package com.this4u.data.engine.plan;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class BatchLoadNode {
    private String parentTable;
    private String childTable;
    private String fkColumn;
    private Long childModuleId;

    // Grandchild relation support
    private boolean hasGrandChild;
    private BatchLoadNode grandChild;

    private Map<Object, List<Map<String, Object>>> loadedRows;
    private Map<Object, List<Map<String, Object>>> grandChildRows;

    public BatchLoadNode(String parentTable, String childTable, String fkColumn, Long childModuleId) {
        this.parentTable = parentTable;
        this.childTable = childTable;
        this.fkColumn = fkColumn;
        this.childModuleId = childModuleId;
    }
}
