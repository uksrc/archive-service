package org.uksrc.archive.utils.tools.tap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.Caom2Model;
import org.ivoa.dm.tapschema.ColNameKeys;
import org.ivoa.dm.tapschema.Schema;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.jboss.logging.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.*;

/*
Adds any existing entities in the database to the TAP_SCHEMA (tables & columns)
 */
@Startup
@ApplicationScoped
public class TapSchemaPopulator {

    @Inject
    EntityManager entityManager;
    @Inject
    TapSchemaRepository tapSchemaRepository;

    private static final Logger LOG = Logger.getLogger(TapSchemaPopulator.class);

    static final String CHECK_TABLE_EXISTS_SQL = "SELECT 1 FROM tap_schema.\"tables\" WHERE table_name = ?";
    static final String CHECK_TAP_DEPLOYED_SQL = "SELECT schema_name FROM tap_schema.schemas;";
    static final String GET_TABLES_FOR_SCHEMA =  "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
    static final String GET_COLUMNS_FOR_TABLE = "SELECT column_name, data_type, udt_name, character_maximum_length FROM information_schema.columns WHERE table_name = ?";
    static final String SCHEMA_NAME = "TAP_SCHEMA";

    @SuppressWarnings("unused")
    @PostConstruct
    public void initialize() {
        try {
            // Add the self-describing information of the TAP SCHEMA to the database.
            // As the tap_schema tables need to contain information about itself as well as the objects that are to be exposed.
            int num = 0;
            try {
                num = entityManager.createNativeQuery(CHECK_TAP_DEPLOYED_SQL).getResultList().size();
            }
            catch (Exception e) {
                String msg = "tap_schema.schemas not found";
                Log.error(msg, e);
                System.out.println(msg);
            }
            if (num == 0){
                addTapSchema();
               // entityManager."alter schema tap_schema rename to \"TAP_SCHEMA\";"
            }

            // Add any objects that need to be visible to the TAP service
      //      addSchemaMembers("public");

        }catch (Exception e) {
            LOG.error("Populating TAP Schema", e);
        }
    }

    /**
     * Add the TAP Schema to the database.
     * Requires the import.sql to be in the resources folder
     */
    private void addTapSchema() {
        try {
            TapschemaModel model = new TapschemaModel();
            InputStream is = TapschemaModel.TAPSchema();
            if (is != null) {
                JAXBContext jc = model.management().contextFactory();
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(new StreamSource(is), TapschemaModel.class);
                TapschemaModel model_in = el.getValue();
                if (model_in != null) {
                    ColNameKeys.normalize(model_in);
                    List<Schema> schemas = model_in.getContent(Schema.class);
                    if (!schemas.isEmpty()) {
                        Schema schema = schemas.get(0);
                        String schemaName = getSchemaName(schema.getSchema_name());
                        schema.setSchema_name(schemaName.toUpperCase());

                        tapSchemaRepository.insertSchema(schema);
                  }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addSchemaMembers(String schemaName) {
        //TODO move CAOM objects to their own schema (Requires library change) -currently gets anything in public which may be an issue later on.
        tapSchemaRepository.insertSchema(schemaName, "default schema", null, null);

        List<?> result = entityManager.createNativeQuery(GET_TABLES_FOR_SCHEMA)
                .setParameter(1, schemaName)
                .getResultList();

        List<String> newTables = result.stream()
                .map(Object::toString)
                .toList();

        for (String tableName : newTables) {
            boolean exists = !entityManager.createNativeQuery(CHECK_TABLE_EXISTS_SQL)
                    .setParameter(1, tableName)
                    .getResultList().isEmpty();

            if (!exists  && !tableName.startsWith("\"")) {
                exists = !entityManager.createNativeQuery(CHECK_TABLE_EXISTS_SQL)
                        .setParameter(1, "\"" + tableName + "\"")
                        .getResultList().isEmpty();
            }

            if (!exists) {
                tapSchemaRepository.addTable("public", tableName, "table", "Details of a(n) " + tableName);

                List<?> colResults = entityManager.createNativeQuery(GET_COLUMNS_FOR_TABLE)
                        .setParameter(1, tableName)
                        .getResultList();

                for (Object columnDetails : colResults) {
                    if (columnDetails instanceof Object[] details) {
                        //TODO - column description source?
                        if (details.length >= 4) {
                            tapSchemaRepository.addColumn(tableName, (String) details[0], (String) details[1], (String) details[2], (Integer) details[3], "colDesc");
                        }
                        else {
                            LOG.warn("TAP Schema Populating - Error adding a column to table " + tableName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility function to remove superfluous xml prefixes on properties.
     * @param columnName raw property string from xml
     * @return string with prefix removed or original string if none present.
     */
    private  String stripXMLPrefix(String columnName) {
        int dotIndex = columnName.indexOf('.');
        if (dotIndex > 0) {
            return columnName.substring(dotIndex + 1);
        }
        return columnName;
    }

    //required for lambda evaluations
    private String getSchemaName(String schemaName){
        return schemaName.compareToIgnoreCase("tapschema") == 0 ? TapSchemaPopulator.SCHEMA_NAME : schemaName;
    }
}
