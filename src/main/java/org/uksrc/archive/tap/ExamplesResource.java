package org.uksrc.archive.tap;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides a RESTful endpoint to serve an HTML representation of example TAP (Table Access Protocol)
 * queries to clients. The HTML content is loaded from the application's classpath and returned
 * as a response in text/html format.
 * <p>
 * Endpoint:
 * - Path: /tap/examples
 * <p>
 * Behaviour:
 * - The method retrieves the "tap/examples.html" resource from the classpath.
 * - If the resource is not present, an {@link IllegalStateException} is thrown.
 * - The HTML content is read and returned as a string in UTF-8 encoding.
 * <p>
 * An exception is thrown if:
 * - The resource cannot be found.
 * - There are issues reading the resource.
 */
@Path("/tap/examples")
public class ExamplesResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getExamples() throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("tap/examples.html")) {
            if (is == null) {
                throw new IllegalStateException("examples.html not found");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
