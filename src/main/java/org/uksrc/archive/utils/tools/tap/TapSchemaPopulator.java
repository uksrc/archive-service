package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.List;

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

    @SuppressWarnings("unused")
    @PostConstruct
    public void initialize() {
        try {
            //For production builds, Schema not added automatically for release builds
            int num = entityManager.createNativeQuery("SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'TAP_SCHEMA';").getResultList().size();
            if (num == 0){
                addTapSchema();
            }

            List<?> result = entityManager.createNativeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'").getResultList();
            List<String> newTables = result.stream()
                    .map(Object::toString)
                    .toList();

            for (String tableName : newTables) {
                boolean exists = !entityManager.createNativeQuery(checkTableExistsSql)
                        .setParameter(1, tableName)
                        .getResultList().isEmpty();

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
            }
        }catch (Exception e) {
            LOG.error("Populating TAP Schema", e);
        }
    }

    private void addTapSchema() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("import.sql");
        if (resource == null) {
            System.out.println("No import.sql found");
        }
        else {
            try {
                tapSchemaRepository.executeSQLFile("import.sql");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
