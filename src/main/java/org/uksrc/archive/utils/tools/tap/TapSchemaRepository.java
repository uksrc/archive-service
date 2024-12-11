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

    private final Map<String, String> typeMapping;

    @PersistenceContext
    EntityManager entityManager;

    static final String insertTableSql = "INSERT INTO \"TAP_SCHEMA\".\"tables\"(schema_name, table_name, table_type, description) VALUES (?, ?, ?, ?)";
    static final String insertColumnSql = "INSERT INTO \"TAP_SCHEMA\".\"columns\"(table_name, column_name, description, datatype, size, arraysize, unit, ucd, principal, std, indexed) VALUES(?,?,?,?,?,?,NULL,NULL,0,1,0)";
    //TODO - add in extra properties xtype & arraysize & add in * for any arrays

    //STILTS' Taplint is case-sensitve it seems CompareMetadataStage.java - compatibleDataTypesOneWay(~)
    //E-MDQ-CTYP-5 Declared/result type mismatch for column photometric in table Environment (BOOLEAN != char) - ERROR seems to be caused by BOOLEAN not being set correctly (Vollt?) and defaulting to 'char' dataType when testing.

    //Vollt TAP restricts data types in ADQLLib::DataType.java - DBDatatype to one of {	SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY,	CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, CIRCLE, POLYGON, REGION, UNKNOWN, UNKNOWN_NUMERIC }
    //So neither BOOLEAN or varchar[] ARRAY will work (potential to convert BOOLEAN to SMALLINT)
    public TapSchemaRepository(){
        typeMapping = new HashMap<>();
        typeMapping.put("character varying", "VARCHAR");
        typeMapping.put("timestamp", "TIMESTAMP");             //precision removal for vollt
      //  typeMapping.put("bool", "BOOLEAN");                 //Doesn't seem to work for BOOLEAN or VARCHAR[]/ARRAY
        typeMapping.put("bool", "SMALLINT");
        typeMapping.put("double precision", "DOUBLE");
     /*   typeMapping.put("decimal", "DECIMAL");
        typeMapping.put("date", "DATE");
        typeMapping.put("varchar", "VARCHAR");
        typeMapping.put("char", "CHAR");
        typeMapping.put("integer", "INTEGER");
        typeMapping.put("bigint", "BIGINT");*/
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
//        if (columnName.contains("productType")){
//            System.out.println(tableName + " " + columnName + " " + dataType + " " + udt + " " + maxLength + " " + description);
//        }
//        if (columnName.contains("bool")){
//            System.out.println(tableName + " " + columnName + " " + dataType + " " + udt + " " + maxLength + " " + description);
//        }
        String dt;
        String arraySize = "";
        if (dataType.contains("ARRAY")){
            System.out.println(tableName + " " + columnName + " " + udt);
            dt = convertUdtToArrayType(udt);
            //NOTE need to find a way to pass "ARRAY" type to vollt
            arraySize = "-1";       //This should be "*" for 'multiple' but Vollt compares it against int
        }
        else {
            dt = getStandardType(dataType);
            arraySize = null;
        }
//        if(tableName.equalsIgnoreCase("Energy")){
//            System.out.println(tableName + " " + columnName + " " + udt + " " + maxLength + " " + description);
//        }
        entityManager.createNativeQuery(insertColumnSql)
                .setParameter(1, tableName)
                .setParameter(2, columnName )
                .setParameter(3, description)
                .setParameter(4, dt.toUpperCase())
                .setParameter(5, maxLength)
                .setParameter(6, arraySize)
                .executeUpdate();
    }

    private String convertUdtToArrayType(String udtName) {
        if (udtName.startsWith("_")) {
            return udtName.substring(1);// + "[]";  // Remove the underscore and add []
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
