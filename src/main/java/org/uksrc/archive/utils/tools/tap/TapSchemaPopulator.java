package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.tap.schema.*;
import org.jboss.logging.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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

    static final String checkTableExistsSql = "SELECT 1 FROM \"TAP_SCHEMA\".\"tables\" WHERE table_name = ?";
    static final String SCHEMA_NAME = "TAP_SCHEMA";

    @SuppressWarnings("unused")
    @PostConstruct
    public void initialize() {
        try {
            //For production builds, Schema not added automatically for release builds
            int num = entityManager.createNativeQuery("SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'TAP_SCHEMA';").getResultList().size();
            if (num == 0){
                addTapSchema();
            }

     /*     List<?> result = entityManager.createNativeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'").getResultList();
            List<String> newTables = result.stream()
                    .map(Object::toString)
                    .toList();

            for (String tableName : newTables) {
                boolean exists = !entityManager.createNativeQuery(checkTableExistsSql)
                        .setParameter(1, tableName)
                        .getResultList().isEmpty();

                if (!exists  && !tableName.startsWith("\"")) {
                    exists = !entityManager.createNativeQuery(checkTableExistsSql)
                            .setParameter(1, "\"" + tableName + "\"")
                            .getResultList().isEmpty();
                }

                if (!exists) {
                    tapSchemaRepository.addTable("public", tableName, "table", "Details of a(n) " + tableName);

                    List<?> colResults = entityManager.createNativeQuery("SELECT column_name, data_type, udt_name, character_maximum_length FROM information_schema.columns WHERE table_name = ?")
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
            }*/
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
                    List<Schema> schemas = model_in.getContent(Schema.class);
                    if (!schemas.isEmpty()) {
                        Schema schema = schemas.get(0);
                        String schemaName = getSchemaName(schema.getSchema_name());
                        schema.setSchema_name(schemaName.toUpperCase());


                        tapSchemaRepository.insertSchema(schema);
                        // Scan the foreign keys to determine any primary keys
           /*             Map<String, Set<String>> primaryKeys = new HashMap<>();
                        schema.getTables().forEach(table -> {
                            // Need to infer the primary keys by evaluating foreign keys from the other tables
                            table.getFkeys().forEach(fkey -> {
                                String targetTable = fkey.getTarget_table().getTable_name();
                                fkey.getColumns().forEach(column -> {
                                    String targetColumn = column.getTarget_column().getColumn_name();+
                                    targetColumn = stripXMLPrefix(targetColumn);
                                    primaryKeys.computeIfAbsent(targetTable, k -> new HashSet<>()).add(targetColumn);
                                });
                            });
                        });
*/                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
