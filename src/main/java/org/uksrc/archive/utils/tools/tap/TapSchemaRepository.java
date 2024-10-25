package org.uksrc.archive.utils.tools.tap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Added to allow the submission of TAP_SCHEMA entities (for "transactional" reasons)
 * TODO - If we replace the import.sql approach to creating the TAP_SCHEMA using a JAR for Entity
 * creation, then we can possibly replace this.
 * As createNativeQuery seems to need isolating from a @PostConstruct bean, so encapsulated here.
 */
@ApplicationScoped
public class TapSchemaRepository {

    @PersistenceContext
    EntityManager entityManager;

    static final String insertTableSql = "INSERT INTO \"TAP_SCHEMA\".\"tables\"(schema_name, table_name, table_type, description) VALUES (?, ?, ?, ?)";
    static final String insertColumnSql = "INSERT INTO \"TAP_SCHEMA\".\"columns\"(table_name, column_name, description, datatype, size, unit, ucd, principal, indexed) VALUES(?,?,?,?,?,NULL,NULL,0,0)";

    @Transactional
    public void addTable(final String schemaName, final String tableName, final String tableType, final String description) {
        entityManager.createNativeQuery(insertTableSql)
                .setParameter(1, schemaName)
                .setParameter(2, tableName)
                .setParameter(3, tableType)
                .setParameter(4, description)
                .executeUpdate();
    }

    //INSERT INTO "TAP_SCHEMA"."columns"(table_name,column_name,description,datatype,"size",unit,ucd,principal,indexed) VALUES (table_record.table_name, c.column_name, NULL, c.data_type, NULL, NULL, NULL, 0, 0);
    @Transactional
    public void addColumn(final String tableName, final String columnName, final String dataType, Integer maxLength, final String description) {
        entityManager.createNativeQuery(insertColumnSql)
                .setParameter(1, tableName)
                .setParameter(2, columnName)
                .setParameter(3, description)
                .setParameter(4, dataType)
                .setParameter(5, maxLength)
                .executeUpdate();
    }
}
