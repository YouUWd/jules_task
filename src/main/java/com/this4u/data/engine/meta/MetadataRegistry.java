package com.this4u.data.engine.meta;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class MetadataRegistry {
    // moduleId -> ModuleMeta
    private volatile Map<Long, ModuleMeta> modulesById;
    private volatile Map<String, ModuleMeta> modulesByCode;
    private volatile Map<Long, List<ModuleMeta>> childrenByParentId;
    private volatile Map<Long, ModuleFieldMeta> fieldsById;
    // tableName -> relation
    private volatile Map<String, List<TableRelationMeta>> relationsByTable;

    @PostConstruct
    public void load() {
        // Mock loading from DB logic for compilation
    }

    public ModuleMeta requireModule(long id) {
        return modulesById != null ? modulesById.get(id) : null;
    }

    public ModuleFieldMeta requireField(long fieldId) {
        return fieldsById != null ? fieldsById.get(fieldId) : null;
    }

    public List<ModuleMeta> findChildren(long moduleId) {
        return childrenByParentId != null ? childrenByParentId.get(moduleId) : List.of();
    }

    public RelationType relationBetween(String tableA, String tableB) {
        if (relationsByTable != null) {
             List<TableRelationMeta> relations = relationsByTable.get(tableA);
             if (relations != null) {
                 for (TableRelationMeta r : relations) {
                     if (r.joinTable().equals(tableB)) {
                         return r.relationType();
                     }
                 }
             }
        }
        return null;
    }

    // Virtual module penetration: Find the closest physical ancestor table
    public String resolvePhysicalAnchor(ModuleMeta module) {
        ModuleMeta cur = module;
        while (cur != null && cur.isVirtual()) {
            cur = requireModule(cur.parentId());
        }
        return cur != null ? cur.primaryTable() : null;
    }
}
