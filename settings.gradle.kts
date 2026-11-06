pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.dev.uksrc.org/repository/maven-public/")
        mavenLocal()
    }

}

rootProject.name="archive-service"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        // Our repository
        maven {
            url= uri("https://repo.dev.uksrc.org/repository/maven-public/")
        }
    }
}

plugins {
    id("org.javastro.build") version "0.2.6"
}