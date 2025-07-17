import java.util.*
import java.nio.file.Paths

val defaultPathProp = "vollt.tap.config.path"

tasks.register("generateVolltWebXml") {
    group = "build"
    description = "Generates the web.xml file required by the Vollt TAP service using application.properties"

    doLast {
        val pathProperty = pathProperty()
        val propertyValue = propertyValue(pathProperty)

        // Template with variable placements
        val templatePath = file("src/main/resources/templates/web.xml.template")
        val templateContent = templatePath.readText()

        // Inject values from application.properties into template
        val tapConfPath = Paths.get(propertyValue, "tap.properties").toString().replace("\\", "/")
        val result = templateContent.replace("\${tapconf}", tapConfPath)

        // Output to folder where the Vollt servlet expects it
        val targetFile = file("src/main/resources/META-INF/web.xml")
        targetFile.parentFile.mkdirs()
        targetFile.writeText(result)

        println("Generated Vollt's web.xml at ${targetFile.path}")
    }
}

/**
 * Returns the correct property depending on build profile (%dev %prod)
 * @return String containing the profile-associated property.
 */
fun pathProperty() : String {
    // Build profile
    val prodTasks = listOf("build", "quarkusBuild", "assemble")

    val isProdTask = gradle.startParameter.taskNames.any { task ->
        prodTasks.any { task.equals(it, ignoreCase = true) }
    }

    val buildProfile = (project.findProperty("quarkus.profile")
        ?: System.getProperty("quarkus.profile"))?.toString()
        ?: if (isProdTask) "prod" else "dev"

    val intendedPathProperty = when (buildProfile) {
        "prod" -> "%prod.${defaultPathProp}"
        "dev" -> "%dev.${defaultPathProp}"
        else -> "${defaultPathProp}"
    }
    return intendedPathProperty
}

/**
 * Gets the actual value of the supplied property.
 * @param pathProperty String contains a valid application.properties property
 * @return String containing the value assigned to the property (if running in dev mode then the path will be prefixed
 * with the java temp directory to avoid any cross-platform write issues)
 */
fun propertyValue(pathProperty: String) : String {
    // Load the application.properties file
    val appProps = Properties()
    val appPropsFile = file("src/main/resources/application.properties")
    if (!appPropsFile.exists()) {
        throw GradleException("application.properties file not found at ${appPropsFile.path}")
    }
    appProps.load(appPropsFile.reader())

    // Required properties
    var pathValue = appProps.getProperty(pathProperty) ?: appProps.getProperty(defaultPathProp)
    if (pathValue == null || pathValue == ""){
        pathValue = System.getProperty("java.io.tmpdir")
    }
    else if (pathProperty().startsWith("%dev") || pathProperty.startsWith("vollt")){
        pathValue = System.getProperty("java.io.tmpdir") + pathValue
        pathValue = Paths.get(pathValue).normalize().toString()
    } else if (pathProperty.startsWith("%prod")) {
        // Prod:
        pathValue = Paths.get(pathValue).normalize().toString().replace("\\", "/")
    }

    return pathValue
}