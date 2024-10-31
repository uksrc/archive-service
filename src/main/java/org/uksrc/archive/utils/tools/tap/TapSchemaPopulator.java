package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

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

    @PostConstruct
    public void initialize() {
        try {
            @SuppressWarnings("unchecked")
            List<String> newTables = entityManager.createNativeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'").getResultList();
            for (String tableName : newTables) {
                boolean exists = !entityManager.createNativeQuery(checkTableExistsSql)
                        .setParameter(1, tableName)
                        .getResultList().isEmpty();

                if (!exists) {
                    tapSchemaRepository.addTable("public", tableName, "table", "Details of a(n) " + tableName);

                    @SuppressWarnings("unchecked")
                    List<Object[]> columns = entityManager.createNativeQuery("SELECT column_name, data_type, udt_name, character_maximum_length FROM information_schema.columns WHERE table_name = ?")
                            .setParameter(1, tableName)
                            .getResultList();

                    for (Object[] column : columns) {
                        tapSchemaRepository.addColumn(tableName, (String) column[0], (String) column[1], (String) column[2], (Integer) column[3], "colDesc");
                    }
                }
            }
        }catch (Exception e) {
            LOG.error("Populating TAP Schema", e);
        }
    }
}
