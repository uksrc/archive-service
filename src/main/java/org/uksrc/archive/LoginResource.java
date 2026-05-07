package org.uksrc.archive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.uksrc.archive.utils.tools.PkceUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

/**
 * Example of the login process, it will call the AuthenticationResource method once logged in (which will request
 * a bearer token).
 */
@Path("")
//IfBuildProfile("dev")
public class LoginResource {
    //"<tokenServerUrl>/logout" if logging out is required for testing.

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String tokenServerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "authentication.callback")
    String authCallbackURI;

    @GET
    @Operation(summary = "Displays a test login page.", description = "Displays a simple login page that will redirect to the OIDC login process. Intended for testing only.")
    @Produces(MediaType.TEXT_HTML)
    public Response loginPage() throws Exception {

        String verifier = PkceUtil.generateVerifier();
        String challenge = PkceUtil.generateChallenge(verifier);

        String state = UUID.randomUUID().toString();

        String redirectUri = URLEncoder.encode(authCallbackURI, StandardCharsets.UTF_8);

        String loginUrl = UriBuilder.fromUri(tokenServerUrl)
                .path("authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "openid profile")
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("state", state)
                .build()
                .toString();

        String html = buildLoginPage(loginUrl);

        NewCookie pkceCookie = new NewCookie.Builder("pkce_verifier")
                .value(verifier)
                .path("/")
                .maxAge(300)
                .httpOnly(false)
                .secure(false)
                .sameSite(NewCookie.SameSite.LAX)
                .build();

        return Response.ok(html).cookie(pkceCookie).build();
    }

    /**
     * Create a simple HTML page that will redirect to the OIDC login process.
     * @param loginUrl The URL to redirect to
     * @return The page to display
     */
    private String buildLoginPage(String loginUrl) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <title>Archive Service Login</title>
        </head>
        <body>
          <h1>Archive Service Login</h1>

          <button id="loginBtn">Login</button>

          <script>
              const loginUrl = "%s";

              document.getElementById("loginBtn").addEventListener("click", function () {
                  window.location.href = loginUrl;
              });
          </script>
        </body>
        </html>
        """.formatted(loginUrl);
    }
}
