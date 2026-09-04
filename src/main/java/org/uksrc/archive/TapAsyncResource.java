package org.uksrc.archive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.javastro.ivoa.quarkus.tap.BaseAsyncTAPResource;
import org.javastro.ivoa.quarkus.tap.TAPHelper;

/**
 * Resource class providing endpoints for asynchronous TAP (Table Access Protocol) queries.
 * This class extends the base functionality of {@code BaseAsyncTAPResource} and supports
 * operations related to handling asynchronous TAP requests.
 * <p>
 * Overrides:
 * - {@code getTapHelper()}: Returns the instance of {@code TAPHelper} injected into this class,
 *   used to assist with the implementation of TAP query services.
 * <p>
 * This resource provides the main entry point for async TAP query operations at the path "/async".
 */
@Tag(name="TAP Query", description = "the TAP query endpoints")
@ApplicationScoped
@Path("tap/async")
public class TapAsyncResource extends BaseAsyncTAPResource {

    @Inject
    TAPHelper tapHelper;

    @Override
    protected TAPHelper getTapHelper() {
        return tapHelper;
    }
}