package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TapPropertiesRenderer
 * Will replace the variable placeholders in the tap.properties template (tap.properties.template) with
 * values from application.properties.
 * The intention here is to pick up the environment variables pass in at runtime.
 */
@Startup
@ApplicationScoped
public class TapPropertiesRenderer {

    @ConfigProperty(name = "vollt.tap.config.path")
    String outputPath;

    private static final Logger LOG = Logger.getLogger(TapPropertiesRenderer.class);

    @PostConstruct
    public void renderTemplate() throws IOException {
        LOG.info("TAP properties rendering started...");
        System.out.println("TAP properties rendering started..." + outputPath);

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

        var config = ConfigProvider.getConfig();

        try (InputStream in = getClass().getResourceAsStream("/templates/tap.properties.template")) {
            if (in == null) throw new FileNotFoundException("/tap.properties.template not found");

            String content = new String(in.readAllBytes());
            String rendered = replacePlaceholdersWithConfig(content, config);

            Path path = Paths.get(rootPath, "tap.properties");
            Files.createDirectories(path.getParent());
            Files.writeString(path, rendered);

            LOG.info("TAP properties saved to " + path);
        }
    }

    private String replacePlaceholdersWithConfig(String input, Config config) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1); // the inside of ${...}
            String value = config.getOptionalValue(key, String.class).orElse("");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
