package org.uksrc.archive;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.javastro.ivoa.entities.resource.Capability;
import org.javastro.ivoa.entities.vosi.capabilities.Capabilities;
import org.javastro.ivoa.quarkus.tap.TAPHelper;
import org.javastro.ivoacore.common.ServiceLocator;
import org.javastro.ivoacore.tap.TAPJob;
import org.javastro.ivoacore.tap.TAPJobSpecification;
import org.javastro.ivoacore.tap.schema.SchemaProvider;
import org.javastro.ivoacore.tap.schema.VODMLSchemaProvider;
import org.javastro.ivoacore.uws.JobManager;
import org.javastro.ivoacore.uws.environment.DefaultEnvironmentFactory;
import org.javastro.ivoacore.uws.environment.DefaultExecutionPolicy;
import org.javastro.ivoacore.uws.environment.EnvironmentFactory;
import org.javastro.ivoacore.uws.persist.CachedJobStore;
import org.javastro.ivoacore.uws.persist.DatabaseJobStore;
import org.javastro.ivoacore.uws.persist.JobStore;
import org.javastro.ivoacore.vosi.CapabilityBuilder;
import org.javastro.ivoacore.vosi.VOSIProvider;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

@ApplicationScoped
public class TapConfiguration {

    @ConfigProperty(name="ivoa.tap.schema")
    String tapSchemaResource;

    @Inject
    DataSource ds;

    @Inject
    //@PersistenceUnit("uwsstore")
    EntityManager em;

    @ConfigProperty(name="ivoa.tap.dbCaseSensitive", defaultValue = "false")
    boolean isDbCaseSensitive;

    @ConfigProperty(name="ivoa.baseAddress", defaultValue = "http://localhost:8080/")
    URI baseURI;


    @Produces
    @Singleton
    ServiceLocator serviceLocator() {
        return new ServiceLocator() {
            @Override
            public URI serviceURI() {
                return baseURI;
            }
        };
    }

    @Produces
    @Singleton
    VOSIProvider vosi() {
        return new VOSIProvider() {
            @Override
            public Capabilities getCapabilities() {
                URL url = null;
                try {
                    url = new URL(baseURI.toURL(), "VOSI");//IMPL - this needs to be the same as the root in {@see org.javastro.ivoa.tap.VOSIResource }
                    // standard VOSI ones
                    final List<Capability> capabilities = CapabilityBuilder.createVOSICapabilities(url);
                    capabilities.addAll(CapabilityBuilder.createTAPCapabilities(baseURI.toURL()));

                    return Capabilities.builder().addCapabilities(capabilities).build();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("base URL is malformed", e);
                }
            }
        };
    }

    @Produces
    @Singleton
    SchemaProvider schema(){
        return new VODMLSchemaProvider(tapSchemaResource, isDbCaseSensitive);
    }

    @Produces
    @Singleton
    JobManager uws(SchemaProvider schemaProvider) {

        File tmpdir = null;
        try {
            tmpdir = Files.createTempDirectory("tapserver").toFile();
        } catch (IOException e) {
            throw new RuntimeException("temporary directory not available", e);
        }

        EnvironmentFactory env = new DefaultEnvironmentFactory(tmpdir);

        TAPJob.JobFactory tapJobFactory = new TAPJob.JobFactory(ds, schemaProvider, env);


        JobStore store = new CachedJobStore(
                new DatabaseJobStore(em,
                        List.of(new NamedType(TAPJobSpecification.class, TAPJob.JOB_TYPE))
                )
        );
        DefaultExecutionPolicy policy = new DefaultExecutionPolicy();
        return new JobManager(tapJobFactory, store, policy);
    }

    @Produces
    @Singleton
    TAPHelper tapHelper(JobManager jobManager, ServiceLocator serviceLocator) {
        return new TAPHelper(jobManager, serviceLocator);
    }

}

