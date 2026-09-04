package org.uksrc.archive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.javastro.ivoa.quarkus.tap.BaseSyncTAPResource;
import org.javastro.ivoa.quarkus.tap.TAPHelper;

/**
 * Provides RESTful endpoints for TAP (Table Access Protocol) synchronous query processing.
 * This resource exposes TAP query operations that comply with the synchronous TAP protocol,
 * designed for querying astronomical data.
 * </p>
 * Properties:
 * - Maintains a configurable timeout defined via the configuration property
 *   {@code ivoa.tap.sync-timeout-seconds} with a default value of 5 seconds.
 * </p>
 * Overrides:
 * - {@code getTapHelper()}: Supplies the TAP helper used for handling query logic.
 * - {@code getSyncWait()}: Provides the configured timeout value for synchronous operations.
 */
@Tag(name = "TAP Query", description = "the TAP query endpoints")
@ApplicationScoped
@Path("tap/sync")
public class TapSyncResource extends BaseSyncTAPResource {

    @ConfigProperty(name="ivoa.tap.sync-timeout-seconds", defaultValue = "5")
    int syncTimeoutSeconds;

    @Inject
    TAPHelper tapHelper;

    @Override
    protected TAPHelper getTapHelper() {
        return tapHelper;
    }

    @Override
    protected int getSyncWait() {
        return syncTimeoutSeconds;
    }
}
