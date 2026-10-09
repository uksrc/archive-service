package org.uksrc.archive.tap;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.javastro.ivoacore.vosi.BaseVOSIResource;
import org.javastro.ivoacore.vosi.VOSIProvider;

/**
 * Provides VOSI (VO Support Interfaces) endpoints for the Table Access Protocol (TAP) service.
 * <p>
 * This resource enables standard VOSI endpoint functionality for TAP, allowing
 * the service to expose metadata, capabilities, and other relevant information
 * in compliance with the VOSI standard.
 */
@Tag(name="VOSI", description = "the standard VOSI endpoints")
@Path("/tap")
public class TapVOSIResource extends BaseVOSIResource {

    @Inject
    public TapVOSIResource(VOSIProvider provider) {
        super(provider);
    }
}
