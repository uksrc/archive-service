package org.uksrc.archive.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.ivoa.dm.caom2.Caom2Model;

public class CustomObjectMapper {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(CustomObjectMapper.class);

    // Replaces the CDI producer for ObjectMapper built into Quarkus
    @Singleton
    @Produces
    ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {
        ObjectMapper mapper = Caom2Model.jsonMapper(); // Custom `ObjectMapper`
        logger.info("custom jackson mapper used");
        // Apply all ObjectMapperCustomizer beans (incl. Quarkus)
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(mapper);
        }

        return mapper;
    }
}
