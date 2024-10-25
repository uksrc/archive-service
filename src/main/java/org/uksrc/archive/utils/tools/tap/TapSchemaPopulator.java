package org.uksrc.archive;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    static final String checkTableExistsSql = "SELECT 1 FROM \"TAP_SCHEMA\".\"tables\" WHERE table_name = ?";

    private static final Logger LOG = LoggerFactory.getLogger(TapSchemaPopulator.class);

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
                    List<Object[]> columns = entityManager.createNativeQuery("SELECT column_name, data_type, character_maximum_length FROM information_schema.columns WHERE table_name = ?")
                            .setParameter(1, tableName)
                            .getResultList();

                    for (Object[] column : columns) {
                        tapSchemaRepository.addColumn(tableName, (String) column[0], (String) column[1], Integer.getInteger((String) column[0]), "colDesc");
                    }

                }
            }

        }catch (Exception e) {
            LOG.error("Populating TAP Schema");
        }

    }
}
