import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

tasks.register("generateWebXml") {
    group = "build setup"
    description = "Generates META-INF/web.xml from templates/web.xml.template"

    doLast {
        val appPropsFile = file("src/main/resources/application.properties")
        val appProps = Properties()
        if (appPropsFile.exists()) appProps.load(appPropsFile.inputStream())

        val outputPath = appProps.getProperty("vollt.tap.config.path")
            ?: System.getenv("VOLLT_TAP_CONFIG_PATH")
            ?: throw GradleException("vollt.tap.config.path not found")

        val templatePath = file("src/main/resources/templates/web.xml.template").toPath()
        if (!Files.exists(templatePath)) {
            throw GradleException("Template not found: $templatePath")
        }

        val home = System.getenv("HOME") ?: System.getProperty("user.home")
        val configDir = Paths.get(home, ".config", outputPath)
        Files.createDirectories(configDir)

        val tapConfPath = configDir.resolve("tap.properties").toString()
        val rendered = Files.readString(templatePath)
            .replace("\${tap.properties.path}", tapConfPath)

        val targetFile = file("build/generated-resources/META-INF/web.xml").toPath()
        Files.createDirectories(targetFile.parent)
        Files.writeString(targetFile, rendered)

        println("✅ Generated: $targetFile")
    }
}