package org.uksrc.archive.auth;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * List of roles (IAM groups) allowed by the resource being requested.
 * @see ConditionalRolesAllowedInterceptor
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ConditionalRolesAllowed {
    @Nonbinding
    String value();
}
