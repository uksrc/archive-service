package org.uksrc.archive.utils.query;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Class to register pgSphere helper functions for Hibernate queries.
 */
public class PgSphereDialect extends PostgreSQLDialect implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();

        var typeConfig = functionContributions.getTypeConfiguration();
        var doubleType = typeConfig.getBasicTypeRegistry()
                .resolve(StandardBasicTypes.DOUBLE);

        // Distance function x,y <-> x,y
        registry.registerPattern(
                "pgsphere_distance",
                "(spoint(radians(?1), radians(?2))::spoint <-> spoint(radians(?3), radians(?4))::spoint)",
                doubleType
        );

    }
}

