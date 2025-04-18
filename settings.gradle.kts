pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
    }
}
rootProject.name="archive-service"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url= uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }

        //TODO - Vollt TAP dependencies from our repo (updated to Jakarta)
        maven {
            url= uri("https://repo.dev.uksrc.org/repository/maven-snapshots/")
        }
    }
}
