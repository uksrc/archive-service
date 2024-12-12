package org.uksrc.archive.utils.tools.tap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Added to allow the submission of TAP_SCHEMA entities (for "transactional" reasons)
 * Performs minor datatype conversions that may be required for compliance with TAP1.0 (Vollt restriction).
 * <p>
 * NOTE: if using STILTS' Taplint, please note that it is case-sensitve it seems see CompareMetadataStage.java - compatibleDataTypesOneWay(~)
 * NOTE: Unrecognised data types are set to CHAR by default, make sure all dataTypes are supported. Vollt TAP restricts data types in ADQLLib::DataType.java -
 * DBDatatype to one of { SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY, CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, CIRCLE, POLYGON, REGION, UNKNOWN, UNKNOWN_NUMERIC }
 * note that there is no BOOLEAN in TAP1.0 (Vollt restriction) and has to be converted to SMALLINT
 * <p>
 * Only tested using CADC's CAOM 2.5 model that has been generated automatically by this framework.
 */
@ApplicationScoped
public class TapSchemaRepository {

    private final Map<String, String> typeMapping;

    @PersistenceContext
    EntityManager entityManager;

    static final String insertTableSql = "INSERT INTO \"TAP_SCHEMA\".\"tables\"(schema_name, table_name, table_type, description) VALUES (?, ?, ?, ?)";
    static final String insertColumnSql = "INSERT INTO \"TAP_SCHEMA\".\"columns\"(table_name, column_name, description, datatype, size, arraysize, unit, ucd, principal, std, indexed) VALUES(?,?,?,?,?,NULL,NULL,NULL,0,1,0)";

    //STILTS' Taplint is case-sensitve it seems CompareMetadataStage.java - compatibleDataTypesOneWay(~)
    //E-MDQ-CTYP-5 Declared/result type mismatch for column photometric in table Environment (BOOLEAN != char) - ERROR seems to be caused by BOOLEAN not being set correctly (Vollt?) and defaulting to 'char' dataType when testing.

    //Vollt TAP restricts data types in ADQLLib::DataType.java - DBDatatype to one of {	SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY,	CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, CIRCLE, POLYGON, REGION, UNKNOWN, UNKNOWN_NUMERIC }
    //So neither BOOLEAN or varchar[] ARRAY will work, need to convert BOOLEAN to SMALLINT (0,1) for example.
    public TapSchemaRepository(){
        typeMapping = new HashMap<>();
        typeMapping.put("character varying", "VARCHAR");
        typeMapping.put("timestamp", "TIMESTAMP");             //precision removal required for vollt
        typeMapping.put("bool", "SMALLINT");                    //Vollt TAP 1_0 doesn't support bool, publisher suggested using 0,1 SMALLINT until TAP1_1
        typeMapping.put("double precision", "DOUBLE");
    }

    /**
     * Adds a table's details to the TAP_SCHEMA in the database.
     * @param schemaName
     * @param tableName
     * @param tableType
     * @param description
     */
    @Transactional
    public void addTable(final String schemaName, final String tableName, final String tableType, final String description) {
        entityManager.createNativeQuery(insertTableSql)
                .setParameter(1, schemaName)
                .setParameter(2, tableName)
                .setParameter(3, tableType)
                .setParameter(4, description)
                .executeUpdate();
    }

    /**
     * Adds a column's details to the TAP_SCHEMA in the database
     * @param tableName
     * @param columnName
     * @param dataType
     * @param udt
     * @param maxLength
     * @param description
     */
    @Transactional
    public void addColumn(final String tableName, final String columnName, final String dataType, final String udt, Integer maxLength, final String description) {
        String dt;
        String size = maxLength.toString();                     //FOR TAP1.0, TAP1.1 should support arraySize if Vollt gets updated.
        if (dataType.contains("ARRAY")){
            System.out.println(tableName + " " + columnName + " " + udt);
            dt = convertUdtToArrayType(udt);
            //NOTE: When 'arraySize' (TAP1.1) used then convert to arraySize and should be "*" for 'multiple' (Vollt compares it against int currently which has been raised as a bug)
            size = "-1";
        }
        else {
            dt = getStandardType(dataType);
        }

        entityManager.createNativeQuery(insertColumnSql)
                .setParameter(1, tableName)
                .setParameter(2, columnName )
                .setParameter(3, description)
                .setParameter(4, dt.toUpperCase())
                .setParameter(5, size)              //"-1" for array or maxlength for other types
                .executeUpdate();
    }

    /**
     * Remove leading underscore from arrays.
     * @param udtName
     * @return
     */
    private String convertUdtToArrayType(String udtName) {
        if (udtName.startsWith("_")) {
            return udtName.substring(1);//Remove the underscore and remember to add either "-1" to size (TAP1.0) or "*" to arraySize(TAP1.1)
        }
        return udtName;
    }

    /**
     * Return the TAP equivalent of the supplied datatype, @see typeMapping
     * @param dataType type supplied by the input data
     * @return The TAP equivalent, could just be to removed superfluous suffix/prefix etc.
     */
    private String getStandardType(String dataType) {
        for (String key : typeMapping.keySet()) {
            if (dataType.contains(key)) {
                return typeMapping.get(key);
            }
        }
        return dataType;
    }
}
