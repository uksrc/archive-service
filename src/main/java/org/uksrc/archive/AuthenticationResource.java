package org.uksrc.archive;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Path("/auth-callback")
public class AuthenticationResource {

    @GET
    public Response handleOAuthCallback(@QueryParam("code") String code, @QueryParam("state") String state) {
        if (code == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing authorization code").build();
        }

        // Process the authorization code (exchange it for an access token)
        String accessToken = exchangeAuthorizationCodeForToken(code);

        //  if (accessToken == null) {
        //      return Response.status(Response.Status.UNAUTHORIZED).entity("Failed to exchange code for token").build();
        //  }

        // Redirect user to a successful login page
        return Response.ok()
                .type(MediaType.TEXT_PLAIN)
                .entity("Success")
                .build(); //.seeOther(URI.create("/success")).build();
    }

    private String exchangeAuthorizationCodeForToken(String code) {
        try {
            String tokenEndpoint = "https://ska-iam.stfc.ac.uk/token";

            String formBody = "grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode("REDACTED", StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode("REDACTED", StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode("http://localhost:8080/auth-callback", StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();

            // Build request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Print response body
            System.out.println(response.body());

            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
