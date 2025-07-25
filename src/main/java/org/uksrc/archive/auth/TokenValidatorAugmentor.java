package org.uksrc.archive.auth;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Used to intercept any tokens used to access the service. Intended to determine if the client ID of the IAM's
 * custom client for the service is correct. Aims to avoid using other IAM services to spoof access.
 */
@ApplicationScoped
public class TokenValidatorAugmentor implements SecurityIdentityAugmentor {

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "")
    String expectedClientId;

    @ConfigProperty(name = "security.roles.enabled", defaultValue = "false")
    boolean securityEnabled;

    private static final Logger LOG = Logger.getLogger(TokenValidatorAugmentor.class);

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        //Skip checking in test due to using dummy tokens
        String profile = ConfigProvider.getConfig().getOptionalValue("quarkus.profile", String.class).orElse("prod");
        if ("test".equals(profile) || !securityEnabled || identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        Optional<TokenCredential> tokenCredentialOpt = identity.getCredentials().stream()
                .filter(cred -> cred instanceof TokenCredential)
                .map(cred -> (TokenCredential) cred)
                .findFirst();

        if (tokenCredentialOpt.isEmpty()) {
            throw new ForbiddenException("Access denied: no access token credential found");
        }

        String token = tokenCredentialOpt.get().getToken();
        String clientId = extractClaim(token)
                .orElseThrow(() -> new ForbiddenException("Access denied: client_id not present in token"));

        if (!expectedClientId.equals(clientId)) {
            LOG.warnf("Invalid client_id: %s", clientId);
            throw new ForbiddenException("Access denied: incorrect client_id in token");
        }

        return Uni.createFrom().item(identity);
    }

    /**
     * Decode the JWT token and attempt to extract the claim_id
     * @param token The supplied JWT token
     * @return Optional<String> containing the value of claim_id if present.
     */
    private Optional<String> extractClaim(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject claims = Json.createReader(new StringReader(payloadJson)).readObject();
            if (claims.containsKey("client_id")) {
                return Optional.of(claims.getString("client_id"));
            }
            return Optional.empty();
        } catch (Exception e) {
            LOG.warn("Failed to decode JWT token claims", e);
            return Optional.empty();
        }
    }

    @Override
    public int priority() {
        return SecurityIdentityAugmentor.super.priority();
    }
}
