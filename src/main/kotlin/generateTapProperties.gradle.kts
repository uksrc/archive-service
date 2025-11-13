import java.util.Properties
import java.util.regex.Pattern
import java.util.regex.Matcher
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

tasks.register("generateTapProperties") {
    group = "build setup"
    description = "Generates tap.properties from template (profile-aware)"

    doLast {
        val props = Properties()

        // Load base application.properties
        val baseProps = file("src/main/resources/application.properties")
        if (baseProps.exists()) props.load(baseProps.inputStream())

        // Detect active profile
        val profile = System.getenv("QUARKUS_PROFILE")
            ?: props.getProperty("quarkus.profile")
            ?: "dev"

        // Merge profile-specific file if it exists
        val profileFile = file("src/main/resources/application-$profile.properties")
        if (profileFile.exists()) props.load(profileFile.inputStream())

        // Add support for Quarkus-style %profile. keys inside base props
        val expandedProps = Properties()
        props.forEach { k, v ->
            val key = k.toString()
            if (key.startsWith("%$profile.")) {
                expandedProps[key.removePrefix("%$profile.")] = v
            } else if (!key.startsWith("%")) {
                expandedProps[key] = v
            }
        }

        val outputPath = expandedProps.getProperty("vollt.tap.config.path")
            ?: System.getenv("VOLLT_TAP_CONFIG_PATH")
            ?: throw GradleException("vollt.tap.config.path not found")

        val templatePath = file("src/main/resources/templates/tap.properties.template").toPath()
        if (!Files.exists(templatePath)) throw GradleException("Template not found: $templatePath")

        val home = System.getenv("HOME") ?: System.getProperty("user.home")
        val configDir = Paths.get(home, ".config", outputPath)
        Files.createDirectories(configDir)

        val template = Files.readString(templatePath)
        val pattern = Pattern.compile("\\$\\{([^}]+)}")
        val matcher = pattern.matcher(template)
        val result = StringBuffer()

        while (matcher.find()) {
            val key = matcher.group(1)
            val value = expandedProps.getProperty(key)
                ?: System.getenv(key)
                ?: throw GradleException("Missing value for template variable: $key")
            matcher.appendReplacement(result, Matcher.quoteReplacement(value))
        }
        matcher.appendTail(result)

        val targetFile = configDir.resolve("tap.properties")
        Files.writeString(targetFile, result.toString())

        println("✅ Generated TAP properties for profile '$profile': $targetFile")
    }
}
