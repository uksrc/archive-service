package org.uksrc.archive;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.Random;

/**
 * Example of the login process, will call the AuthenticationResource method once logged in (which will request
 * a bearer token).
 */
@Path("/")
//IfBuildProfile("dev")
public class LoginResource {
    //"<tokenServerUrl>/logout" if logging out is required for testing.

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String tokenServerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @GET
    @Operation(summary = "Displays a test login page.", description = "Displays a simple login page that will redirect to the OIDC login process. Intended for testing only.")
    @Produces(MediaType.TEXT_HTML)
    public String loginPage() {
        String state = Long.toString(new Random().nextLong(), 36).substring(7);

        return String.format("""
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
                        const url = "%s/authorize" +
                            "?response_type=code" +
                            "&client_id=%s" +
                            "&redirect_uri=" + encodeURIComponent(window.location.origin + "/auth-callback") +
                            "&audience=authn-api" +
                            "&scope=openid+profile+offline_access" +
                            "&state=" + "%s";
                            window.location.href = url;
                   }
                </script>""", tokenServerUrl, clientId, state);
    }
}
