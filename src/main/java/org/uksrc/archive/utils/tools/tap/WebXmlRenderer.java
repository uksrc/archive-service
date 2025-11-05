package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates the web.xml for the Vollt servlet.
 * Takes platform path issues into account
 */
@Startup
@ApplicationScoped
public class WebXmlRenderer {

    @ConfigProperty(name = "vollt.tap.config.path")
    String outputPath;

    @PostConstruct
    public void renderTemplate() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/templates/web.xml.template")) {
            if (in == null) throw new FileNotFoundException("/templates/web.xml.template not found");

            String content = new String(in.readAllBytes());

            String rootPath = "";
            List<String> activeProfiles = ConfigUtils.getProfiles();
            String primaryProfile = activeProfiles.isEmpty() ? null : activeProfiles.get(0);
            if (primaryProfile != null && !primaryProfile.equals("prod")) {
                Path configDir = Paths.get(System.getProperty("user.home"), ".config", outputPath);
                Files.createDirectories(configDir);
                rootPath = configDir.toString();
            }
            else {
                rootPath = outputPath;
            }

            String tapConfPath = Paths.get(rootPath, "tap.properties").toString();
            String rendered = content.replace("${tap.properties.path}", tapConfPath);

            // Inject values from application.properties into template
            Path projectRoot = Paths.get("").toAbsolutePath(); // current working dir
            if (primaryProfile != null && !primaryProfile.equals("test")) {
                projectRoot = projectRoot.getParent().getParent().getParent().getParent();
            }

            Path targetFile = projectRoot.resolve("src/main/resources/META-INF/web.xml");
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, rendered);
        }
    }
}