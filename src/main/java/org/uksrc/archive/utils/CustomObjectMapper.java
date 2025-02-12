package org.uksrc.archive.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.ivoa.dm.caom2.Caom2Model;
import org.jboss.logging.Logger;

/**
 * Helper class for the automatic xml/json to Java object
 */
public class CustomObjectMapper {
    private static final Logger log = Logger.getLogger(CustomObjectMapper.class);

    // Replaces the CDI producer for ObjectMapper built into Quarkus
    @SuppressWarnings("unused")
    @Singleton
    @Produces
    ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {
        ObjectMapper mapper = Caom2Model.jsonMapper(); // Custom `ObjectMapper`
        log.info("custom jackson mapper used");
        // Apply all ObjectMapperCustomizer beans (incl. Quarkus)
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(mapper);
        }

        return mapper;
    }
}
