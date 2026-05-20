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
        val appPropsFile = file("src/main/resources/application.properties")
        val appProps = Properties()
        if (appPropsFile.exists()) appProps.load(appPropsFile.inputStream())

        //Set VOLLT_TAP_CONFIG_PATH for production builds
        val envPath: String? = System.getenv("VOLLT_TAP_CONFIG_PATH")

        val outputPath: String = envPath
            ?: appProps.getProperty("vollt.tap.config.path")
            ?: throw GradleException("vollt.tap.config.path not found")

        val templatePath = file("src/main/resources/templates/tap.properties.template").toPath()
        if (!Files.exists(templatePath)) throw GradleException("Template not found: $templatePath")

        var configDir: java.nio.file.Path
        if (envPath == null) {
            val home = System.getenv("HOME") ?: System.getProperty("user.home")
            configDir = Paths.get(home, ".config", outputPath)
            Files.createDirectories(configDir)
        }
        else {
            configDir = Paths.get(envPath)
            Files.createDirectories(configDir)
        }

        val template = Files.readString(templatePath)
        val pattern = Pattern.compile("\\$\\{([^}]+)}")
        val matcher = pattern.matcher(template)
        val result = StringBuffer()

        while (matcher.find()) {
            val key = matcher.group(1)
            val value = appProps.getProperty(key)
                ?: System.getenv(key)
                ?: throw GradleException("Missing value for template variable: $key")
            matcher.appendReplacement(result, Matcher.quoteReplacement(value))
        }
        matcher.appendTail(result)

        val targetFile = configDir.resolve("tap.properties")
        Files.writeString(targetFile, result.toString())

        println("✅ Generated TAP properties $targetFile")
    }
}
