import java.util.*

// Reads the database details from application.properties and injects them into tap.properties for Vollt TAP service.
// Uses src/main/templates/tapProperties.txt as a template.
// Any update to the database URL (in application.properties) scan be fed through to the tap service which requires an explict database URL.
tasks.register("generateTapProperties") {
    group = "build"
    description = "Generates the tap.properties file based on application.properties"

    var jdbcProperty = "quarkus.datasource.jdbc.url"

    doFirst {
        val executedTasks = project.gradle.startParameter.taskNames
        val isBuild = "build" in executedTasks || executedTasks.any { it.contains("assemble") || it.contains("package") }
        println("isBuild " + isBuild)


        if ("quarkusDev" in executedTasks) {
            jdbcProperty = "dev.quarkus.datasource.jdbc.url"
        }
        else if (isBuild) {
            val profile = project.findProperty("quarkus.profile") ?: "prod"
            println("profile " + profile)
            jdbcProperty = when (profile) {
                "dev" -> "%dev.quarkus.datasource.jdbc.url"
                "prod" -> "%prod.quarkus.datasource.jdbc.url"
                else -> "quarkus.datasource.jdbc.url"
            }
        }
        println("jdbcProperty : " + jdbcProperty)
    }

    doLast {
        // Load the application.properties file
        val appProps = Properties()
        val appPropsFile = file("src/main/resources/application.properties")
        if (!appPropsFile.exists()) {
            throw GradleException("application.properties file not found at ${appPropsFile.path}")
        }
        appProps.load(appPropsFile.reader())

        // Required properties from application.properties
        val jdbcUrl = appProps.getProperty(jdbcProperty) ?: "jdbc:postgresql://localhost:5432/quarkus"
        val username = appProps.getProperty("quarkus.datasource.username") ?: "quarkus"
        val password = appProps.getProperty("quarkus.datasource.password") ?: "quarkus"
        println("dbase API: " + jdbcUrl)

        // Template with variable placements
        val templatePath = file("src/main/resources/templates/tapProperties.txt")
        val templateContent = templatePath.readText()

        // Inject values from application.properties into template
        val result = templateContent.replace("{db_url}", jdbcUrl)
            .replace("{db_user}", username)
            .replace("{db_password}", password)

        // Output to build folder where the Vollt servlet expects it
        val targetFile = file("src/main/resources/tap.properties")
        targetFile.parentFile.mkdirs()
        targetFile.writeText(result)

        println("Generated tap.properties at ${targetFile.path}")
    }
}