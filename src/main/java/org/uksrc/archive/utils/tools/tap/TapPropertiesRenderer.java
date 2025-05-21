package org.uksrc.archive.utils.tools.tap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TapPropertiesRenderer
 * Will replace the variable placeholders in the tap.properties template (tapProperties.txt) with
 * values from application.properties.
 * The intention here is to pick up the environment variables pass in at runtime.
 */
@Startup
@ApplicationScoped
public class TapPropertiesRenderer {

    @PostConstruct
    public void renderTemplate() throws IOException {
        var config = ConfigProvider.getConfig();

        try (InputStream in = getClass().getResourceAsStream("/templates/tapProperties.txt")) {
            if (in == null) throw new FileNotFoundException("/tapProperties.txt not found");

            String content = new String(in.readAllBytes());
            String rendered = replacePlaceholdersWithConfig(content, config);

            //config folder expected to be created by k8s volumeMount
            Path path = Paths.get("/config/tap.properties");
            Files.writeString(path, rendered);
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
