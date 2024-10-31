package org.uksrc.archive.utils.tools.tap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Added to allow the submission of TAP_SCHEMA entities (for "transactional" reasons)
 * TODO - If we replace the import.sql approach to creating the TAP_SCHEMA using a JAR for Entity
 * creation, then we can possibly replace this.
 * As createNativeQuery seems to need isolating from a @PostConstruct bean, so encapsulated here.
 */
@ApplicationScoped
public class TapSchemaRepository {

    private Map<String, String> typeMapping;

    @PersistenceContext
    EntityManager entityManager;

    static final String insertTableSql = "INSERT INTO \"TAP_SCHEMA\".\"tables\"(schema_name, table_name, table_type, description) VALUES (?, ?, ?, ?)";
    static final String insertColumnSql = "INSERT INTO \"TAP_SCHEMA\".\"columns\"(table_name, column_name, description, datatype, size, unit, ucd, principal, std, indexed) VALUES(?,?,?,?,?,NULL,NULL,0,1,0)";

    public TapSchemaRepository(){
        typeMapping = new HashMap<>();
        typeMapping.put("character varying", "varchar");
        typeMapping.put("timestamp", "timestamp");
       // typeMapping.put("boolean", "BOOL");
        typeMapping.put("double precision", "double");
    }

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
    public void addColumn(final String tableName, final String columnName, final String dataType, final String udt, Integer maxLength, final String description) {
        if (columnName.contains("productType")){
            System.out.println(tableName + " " + columnName + " " + dataType + " " + udt + " " + maxLength + " " + description);
        }
        if (columnName.contains("bool")){
            System.out.println(tableName + " " + columnName + " " + dataType + " " + udt + " " + maxLength + " " + description);
        }
        String dt = dataType;
        if (dt.contains("ARRAY")){
            System.out.println(tableName + " " + columnName + " " + udt);
            dt = convertUdtToArrayType(udt);
        }
        else {
            dt = getStandardType(dataType);
        }
        entityManager.createNativeQuery(insertColumnSql)
                .setParameter(1, tableName)
                .setParameter(2, columnName )
                .setParameter(3, description)
                .setParameter(4, dt)
                .setParameter(5, maxLength)
                .executeUpdate();
    }

    private String convertUdtToArrayType(String udtName) {
        if (udtName.startsWith("_")) {
            return udtName.substring(1) + "[]";  // Remove the underscore and add []
        }
        return udtName;  // Return as is if it's not an array
    }

    public String getStandardType(String dataType) {
        // Iterate over the keys to check if any of them are contained in dataType
        for (String key : typeMapping.keySet()) {
            if (dataType.contains(key)) {
                return typeMapping.get(key);  // Return the mapped standard type
            }
        }
        return dataType;  // Return the original type if no match is found
    }
}
