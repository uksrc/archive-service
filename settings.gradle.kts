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
            url= uri("https://central.sonatype.com/repository/maven-snapshots/")
        }

        // Our repository
        maven {
            url= uri("https://repo.dev.uksrc.org/repository/maven-snapshots/")
        }
    }
}
