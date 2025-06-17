package org.uksrc.archive.auth;

import io.quarkus.security.ForbiddenException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Interceptor
@ConditionalRolesAllowed("")
@Priority(Interceptor.Priority.APPLICATION)
public class ConditionalRolesAllowedInterceptor {

    @ConfigProperty(name = "security.roles.enabled", defaultValue = "true")
    boolean rolesEnabled;

    @Inject
    Config config;

    @Inject
    JsonWebToken jwt;

    @AroundInvoke
    public Object enforceRoles(InvocationContext ctx) throws Exception {
        if (!rolesEnabled) return ctx.proceed();
        else throw new ForbiddenException("Access denied: required roles not present");

        // throw new ForbiddenException("Access denied: required roles not present");
        // OR
        // throw new NotAuthorizedException("Bearer");
    }
}
