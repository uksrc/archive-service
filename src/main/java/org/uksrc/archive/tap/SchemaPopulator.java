package org.uksrc.archive.tap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.javastro.ivoacore.tap.schema.SchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SchemaPopulator is responsible for initializing and populating the database schema for the
 * TAP (Table Access Protocol) service during application startup.
 * <p>
 * This class observes the startup event and ensures the database is populated with an initial
 * set of schemas if none exist. The schemas are sourced from the {@code SchemaProvider}.
 */
@ApplicationScoped
public class SchemaPopulator {

    private static final Logger log = LoggerFactory.getLogger(SchemaPopulator.class);

    @PersistenceContext
    EntityManager em;

    @Inject
    SchemaProvider schemaProvider;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        Long i = em.createQuery("select count(o) from Schema o", Long.class).getSingleResult();
        if(i == 0) {
            log.info("populating tap schema");
            for(var s: schemaProvider.getSchemas()) {
                log.info("adding "+s.getSchema_name());
                em.persist(s);
            }
        }
    }
}
