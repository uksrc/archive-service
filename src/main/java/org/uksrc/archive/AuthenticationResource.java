package org.uksrc.archive;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Used as a test resource to allow the handling of authentication requests.
 * Has a method available to retrieve a bearer token from an auth->token chain of requests (redirect_uri) from:
 * https://<YOUR.OIDC.SERVER>/authorize?response_type=code&client_id=<YOUR.CLIENT.ID>&redirect_uri=<YOUR.HOST>/auth-callback
 * &audience=authn-api&scope=openid+profile+offline_access&state=<STATE>
 */
@Path("/auth-callback")
@IfBuildProfile(anyOf = {"dev", "test"})
public class AuthenticationResource {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String tokenServerUrl;

    @ConfigProperty(name = "OIDC_CLIENT_ID")
    String clientId;

    @ConfigProperty(name = "OIDC_CLIENT_SECRET")
    String clientSecret;

    @ConfigProperty(name = "authentication.callback")
    String authUrl;

    private static final Logger LOG = Logger.getLogger(AuthenticationResource.class);

    @GET
    public Response handleOAuthCallback(@QueryParam("code") String code, @QueryParam("state") String state) {
        if (code == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing authorisation code").build();
        }

        // Process the authorization code (exchange it for an access token)
        String accessToken = exchangeAuthorizationCodeForToken(code);
        if (accessToken != null) {
            JsonObject jsonObject = Json.createReader(new StringReader(accessToken)).readObject();
            String bearerToken = jsonObject.getString("access_token");
            System.out.println(bearerToken);

            return Response.ok()
                    .type(MediaType.TEXT_PLAIN)
                    .entity(bearerToken)
                    .build();
        }
        else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Failed to exchange code for token").build();
        }
    }

    private String exchangeAuthorizationCodeForToken(String code) {
        try {
            String tokenEndpoint = tokenServerUrl + "/token";

            String formBody = "grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(authUrl, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();

            // Build request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }
}
