// Determines which web.xml version to use, the dev version uses the default tap.properties and
// the prod version should point to an externally supplied location (see ../config/web-prod.xml tapconf param)
// Intended for use with a docker image where the tap.properties is added to the image with the settings as required.
val prodTasks = listOf("build", "quarkusBuild", "assemble") // Add more if needed

val isProdTask = gradle.startParameter.taskNames.any { task ->
    prodTasks.any { task.equals(it, ignoreCase = true) }
}

val buildProfile = (project.findProperty("quarkus.profile")
    ?: System.getProperty("quarkus.profile"))?.toString()
    ?: if (isProdTask) "prod" else "dev"

val selectedWebXml = when (buildProfile) {
    "prod" -> "web-prod.xml"
    "dev" -> "web-dev.xml"
    else -> "web-dev.xml"
}

val sourceFile = file("src/main/config/$selectedWebXml")
val targetFile = file("src/main/resources/META-INF/web.xml")

tasks.register("copySelectedWebXml") {
    doLast {
        println("Copying $selectedWebXml to META-INF/web.xml...")
        targetFile.parentFile.mkdirs()
        sourceFile.copyTo(targetFile, overwrite = true)
    }
}

// Ensure this runs before resources are processed
tasks.named("processResources") {
    dependsOn("copySelectedWebXml")
}
