package org.uksrc.archive.auth;

import io.quarkus.security.ForbiddenException;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;
import java.util.Set;

@Interceptor
@ConditionalRolesAllowed("")
@Priority(Interceptor.Priority.APPLICATION)
public class ConditionalRolesAllowedInterceptor {

    @ConfigProperty(name = "security.roles.enabled", defaultValue = "true")
    boolean rolesEnabled;

    @Inject
    Config config;

    @Default
    @Inject
    JsonWebToken jwt;

    @AroundInvoke
    public Object enforceRoles(InvocationContext ctx) throws Exception {
        if (rolesEnabled) {
            if (jwt != null) {
                ConditionalRolesAllowed annotation = ctx.getMethod().getAnnotation(ConditionalRolesAllowed.class);
                if (annotation == null) {
                    annotation = ctx.getMethod().getDeclaringClass().getAnnotation(ConditionalRolesAllowed.class);
                }

                if (annotation != null && !annotation.value().isEmpty()) {
                    String configKey = annotation.value();
                    Optional<String> rolesCsvOpt = config.getOptionalValue(configKey, String.class);

                    if (rolesCsvOpt.isPresent()) {
                        Set<String> requiredRoles = Set.of(rolesCsvOpt.get().split("\\s*,\\s*"));

                        for (String role : requiredRoles) {
                            Set<String> groups = jwt.getGroups();
                            if (groups != null && groups.contains(role)) {
                                return ctx.proceed(); // Role matched
                            }
                        }
                        throw new ForbiddenException("Access denied: required roles not present");
                    }
                    else {
                        // No restrictive roles enabled
                        return ctx.proceed();
                    }
                }
                else {
                    // Allow access if there is no restrictive config key provided
                    // (@see ConditionalRolesAllowed) on the resource being called.
                    return ctx.proceed();
                }
            }
            else {
                throw new NotAuthorizedException("Bearer token not present");
            }
        }
        else {
            // Authorisation not enabled, allow access
            return ctx.proceed();
        }



        // throw new ForbiddenException("Access denied: required roles not present");
        // OR
        // throw new NotAuthorizedException("Bearer");
    }
}
