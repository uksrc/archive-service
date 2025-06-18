package org.uksrc.archive.auth;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;
import java.util.Set;

/**
 * Replacement for the @RolesAllowed annotation that will determine which groups the caller
 * has to be in to access the resource.
 * Caller has to supply a bearer token and be a member of whichever group(s) is/are defined in
 * the application.properties and added to the resource
 * with @ConditionalRolesAllowed("<application.properties setting that resolves to the desired group>")
 */
@Interceptor
@ConditionalRolesAllowed("")
@Priority(Interceptor.Priority.APPLICATION)
public class ConditionalRolesAllowedInterceptor {

    @ConfigProperty(name = "security.roles.enabled", defaultValue = "true")
    boolean rolesEnabled;

    @Inject
    Config config;

    @Inject
    SecurityIdentity identity;

    @AroundInvoke
    public Object enforceRoles(InvocationContext ctx) throws Exception {
        if (rolesEnabled) {
            if (identity != null && !identity.isAnonymous()) {
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
                            if (identity.hasRole(role)) {
                                return ctx.proceed();
                            }
                        }
                        throw new ForbiddenException("Access denied: required roles not present");
                    }
                }
            }
            else {
                throw new AuthenticationFailedException("Bearer token required");

            }
        }

        // Catch all, no API restriction, enforcement disabled in application.properties
        return ctx.proceed();
    }
}
