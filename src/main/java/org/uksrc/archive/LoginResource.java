package org.uksrc.archive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.uksrc.archive.utils.tools.PkceUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

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
        String state = Long.toString(new Random().nextLong(), 36).substring(7);

        String loginUrl = String.format("%s/authorize" +
                        "?response_type=code" +
                        "&client_id=%s" +
                        "&redirect_uri=%s" +
                        "&scope=openid+profile" +
                        "&code_challenge=%s" +
                        "&code_challenge_method=S256" +
                        "&state=%s",
                tokenServerUrl, clientId, URLEncoder.encode(authCallbackURI, StandardCharsets.UTF_8),
                challenge, state);

        String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                  <title>Archive Service Login</title>
                </head>
                <body>
                <h1>Archive Service Login</h1>
                  <button onclick="loginFunc()">Login</button>
                </body>
                </html>\
                <script>
                    function loginFunc(){
                        const url = "%s";
                            window.location.href = url;
                   }
                </script>""", loginUrl);

        // Store verifier in a temporary cookie so the callback can read it
        //NewCookie pkceCookie = new NewCookie("pkce_verifier", verifier, "/auth-callback", null, null, 300, false);
        NewCookie pkceCookie = new NewCookie.Builder("pkce_verifier")
                .value(verifier)
                .path("/")             // Set path to root so all resources can see it
                .maxAge(300)           // 5 minutes
                .httpOnly(false)       // Allow browser/server to handle it easily
                .secure(false)         // Don't require HTTPS for localhost
                .sameSite(NewCookie.SameSite.LAX)
                .build();

        return Response.ok(html).cookie(pkceCookie).build();
    }
}
