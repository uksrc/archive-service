package org.uksrc.archive.utils.tools.tap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.*;

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

    //Columns that are reserved words in TAP (currently only CAOM 2.5 entries) - raised with CADC to see if a model adjustment is in order before release.
    static final Set<String> reservedWords = Set.of("coordsys", "pi", "position", "time");

    //STILTS' Taplint is case-sensitive  CompareMetadataStage.java - compatibleDataTypesOneWay(~)
    //Vollt TAP restricts data types in ADQLLib::DataType.java - DBDatatype to one of {	SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY,	CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, CIRCLE, POLYGON, REGION, UNKNOWN, UNKNOWN_NUMERIC }
    //So neither BOOLEAN or varchar[] ARRAY will work, need to convert BOOLEAN to SMALLINT (0,1) for example (TAP 1.0 restriction).
    public TapSchemaRepository(){
        typeMapping = new HashMap<>();
        typeMapping.put("character varying", "VARCHAR");
        typeMapping.put("timestamp", "TIMESTAMP");             //precision removal required for vollt
        typeMapping.put("bool", "SMALLINT");                    //Vollt TAP 1_0 doesn't support bool, publisher suggested using 0,1 SMALLINT until TAP1_1
        typeMapping.put("double precision", "DOUBLE");
    }

    /**
     * Adds a table's details to the TAP_SCHEMA in the database.
     * @param schemaName The schema that this table belongs to.
     * @param tableName The name of the table to be added to the TAP_SCHEMA
     * @param tableType One of either "table" or "view"
     * @param description Human-readable description of the table
     */
    @Transactional
    public void addTable(final String schemaName, final String tableName, final String tableType, final String description) {
        boolean reserved = reservedWords.contains(tableName.toLowerCase(Locale.ROOT));
        entityManager.createNativeQuery(insertTableSql)
                .setParameter(1, schemaName)
                .setParameter(2, reserved ? "\"" + tableName + "\"" : tableName)
                .setParameter(3, tableType)
                .setParameter(4, description)
                .executeUpdate();
    }

    /**
     * Adds a column's details to the TAP_SCHEMA in the database
     * @param tableName The name of the table that the column belongs to
     * @param columnName The name of the column as it appears in the table
     * @param dataType The type of data stored in the column, such as SMALLINT or VARCHAR
     * @param udt The user-defined type, for example the actual data type for when a dataType is ARRAY
     * @param maxLength maximum length of the data
     * @param description Human-readable description of the table
     */
    @Transactional
    public void addColumn(final String tableName, final String columnName, final String dataType, final String udt, Integer maxLength, final String description) {
        String dt;
        Integer size = maxLength;                     //FOR TAP1.0, TAP1.1 should support arraySize if Vollt gets updated.
        if (dataType.contains("ARRAY")){
            dt = convertUdtToArrayType(udt);
            //NOTE: When 'arraySize' (TAP1.1) used then convert to arraySize and should be "*" for 'multiple' (Vollt compares it against int currently which has been raised as a bug)
            size = -1;  // -1 signifies an array
        }
        else {
            dt = getStandardType(dataType);
        }

        boolean reservedCol = reservedWords.contains(columnName.toLowerCase(Locale.ROOT));
        boolean reservedTab = reservedWords.contains(tableName.toLowerCase(Locale.ROOT));
        entityManager.createNativeQuery(insertColumnSql)
                .setParameter(1, reservedTab ? "\"" + tableName + "\"" : tableName)
                .setParameter(2, reservedCol ? "\"" + columnName + "\"" : columnName)
                .setParameter(3, description)
                .setParameter(4, dt.toUpperCase())
                .setParameter(5, size)              //"-1" for array or maxlength for other types
                .executeUpdate();
    }

    /**
     * Remove leading underscore from arrays.
     * @param udtName formats a udt if required
     * @return formatted version
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
