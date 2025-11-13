package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Startup
@ApplicationScoped
public class WebXmlRenderer {

    private static final Logger LOG = Logger.getLogger(WebXmlRenderer.class);

    @ConfigProperty(name = "vollt.tap.config.path")
    String outputPath;

    @PostConstruct
    public void renderTemplate() throws IOException {
        LOG.info("Starting web.xml rendering...");

        // Load the template
        try (InputStream in = getClass().getResourceAsStream("/templates/web.xml.template")) {
            if (in == null) {
                throw new FileNotFoundException("/templates/web.xml.template not found");
            }

            String template = new String(in.readAllBytes());

            // Determine config directory location (same logic as Gradle task)
            String home = System.getenv("HOME");
            if (home == null || home.isEmpty()) {
                home = System.getProperty("user.home");
            }

            Path configDir = Paths.get(home, ".config", outputPath);
            Files.createDirectories(configDir);

            // Construct path to tap.properties
            Path tapConfPath = configDir.resolve("tap.properties");

            // Replace placeholder
            String rendered = template.replace("${tap.properties.path}", tapConfPath.toString());

            // In runtime (Docker, K8s, etc.), we follow Gradle’s output convention
            Path targetFile = Paths.get("build/generated-resources/META-INF/web.xml");
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, rendered);

            LOG.infof("✅ web.xml generated at: %s", targetFile);
        }
    }
}
