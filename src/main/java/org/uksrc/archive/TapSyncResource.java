package org.uksrc.archive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.javastro.ivoa.quarkus.tap.BaseSyncTAPResource;
import org.javastro.ivoa.quarkus.tap.TAPHelper;

@Tag(name = "TAP Query", description = "the TAP query endpoints")
@ApplicationScoped
@Path("sync")
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


