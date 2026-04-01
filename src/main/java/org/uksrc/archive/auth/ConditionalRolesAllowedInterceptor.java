package org.uksrc.archive.auth;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
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
    Instance<SecurityIdentity> identityInstance;

    @AroundInvoke
    public Object enforceRoles(InvocationContext ctx) throws Exception {

        if (!rolesEnabled) {
            return ctx.proceed();
        }

        SecurityIdentity identity = identityInstance.get();

        if (identity == null || identity.isAnonymous()) {
            throw new AuthenticationFailedException("Authentication required");
        }

        ConditionalRolesAllowed annotation =
                ctx.getMethod().getAnnotation(ConditionalRolesAllowed.class);

        if (annotation == null) {
            annotation = ctx.getMethod()
                    .getDeclaringClass()
                    .getAnnotation(ConditionalRolesAllowed.class);
        }

        if (annotation != null && !annotation.value().isEmpty()) {

            Optional<String> rolesCsvOpt =
                    config.getOptionalValue(annotation.value(), String.class);

            if (rolesCsvOpt.isPresent()) {

                Set<String> requiredRoles =
                        Set.of(rolesCsvOpt.get().split("\\s*,\\s*"));

                for (String role : requiredRoles) {
                    if (identity.hasRole(role)) {
                        return ctx.proceed();
                    }
                }

                throw new ForbiddenException("Access denied: required roles not present");
            }
        }

        return ctx.proceed();
    }
}
