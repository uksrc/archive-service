package org.uksrc.archive.tap;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.javastro.ivoa.entities.vosi.tables.Tableset;
import org.javastro.ivoacore.tap.schema.SchemaProvider;

/**
 * Resource class that represents the VOSI (Virtual Observatory Support Interface) Tables endpoint.
 * This class provides an API to retrieve metadata about available tables in compliance with the
 * VOSI specifications.
 * <p>
 * The endpoint serves data in XML format and is typically consumed by Virtual Observatory
 * clients. It uses the {@link SchemaProvider} to fetch and structure table metadata into a
 * VOSI-compliant {@link Tableset} response.
 */
@Tag(name="Table", description = "the standard VOSI Tables endpoint")
@Path("/tap/tables")
public class VOSITablesResource {

    @Inject
    SchemaProvider schemaProvider;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Tableset tables() {
        return schemaProvider.asVOSI();
    }
}
