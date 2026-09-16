package com.this4u.data.engine.meta;

import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetadataRegistry {
    private final JdbcTemplate jdbcTemplate;

    // moduleId -> ModuleMeta
    private volatile Map<Long, ModuleMeta> modulesById;
    private volatile Map<String, ModuleMeta> modulesByCode;
    private volatile Map<Long, List<ModuleMeta>> childrenByParentId;
    private volatile Map<Long, ModuleFieldMeta> fieldsById;
    // tableName -> relation
    private volatile Map<String, List<TableRelationMeta>> relationsByTable;

    public MetadataRegistry(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void load() {
        modulesById = new HashMap<>();
        modulesByCode = new HashMap<>();
        childrenByParentId = new HashMap<>();
        fieldsById = new HashMap<>();
        relationsByTable = new HashMap<>();

        // Load Modules
        jdbcTemplate.query("SELECT id, module_code, module_name, primary_table, parent_id FROM sys_module", rs -> {
            ModuleMeta meta = new ModuleMeta(
                rs.getLong("id"),
                rs.getString("module_code"),
                rs.getString("module_name"),
                rs.getString("primary_table"),
                rs.getLong("parent_id")
            );
            modulesById.put(meta.id(), meta);
            modulesByCode.put(meta.moduleCode(), meta);
            childrenByParentId.computeIfAbsent(meta.parentId(), k -> new ArrayList<>()).add(meta);
        });

        // Load Fields
        jdbcTemplate.query("SELECT id, module_id, table_name, column_name, display_name FROM sys_module_field", rs -> {
            ModuleFieldMeta field = new ModuleFieldMeta(
                rs.getLong("id"),
                rs.getLong("module_id"),
                rs.getString("table_name"),
                rs.getString("column_name"),
                rs.getString("display_name")
            );
            fieldsById.put(field.id(), field);
        });

        // Load Relations
        jdbcTemplate.query("SELECT id, main_table, main_field, join_table, join_field, relation_type FROM sys_table_relation", rs -> {
            TableRelationMeta rel = new TableRelationMeta(
                rs.getLong("id"),
                rs.getString("main_table"),
                rs.getString("main_field"),
                rs.getString("join_table"),
                rs.getString("join_field"),
                RelationType.valueOf(rs.getString("relation_type"))
            );
            relationsByTable.computeIfAbsent(rel.mainTable(), k -> new ArrayList<>()).add(rel);
        });
    }

    public ModuleMeta requireModule(long id) {
        return modulesById != null ? modulesById.get(id) : null;
    }

    public ModuleFieldMeta requireField(long fieldId) {
        return fieldsById != null ? fieldsById.get(fieldId) : null;
    }

    public List<ModuleMeta> findChildren(long moduleId) {
        return childrenByParentId != null ? childrenByParentId.getOrDefault(moduleId, List.of()) : List.of();
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
