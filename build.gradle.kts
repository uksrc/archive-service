plugins {
    java
    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation("io.quarkus:quarkus-undertow")
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("org.opencadc:CAOM:2.5.0-SNAPSHOT:quarkus")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-resteasy-reactive-jaxb")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-kubernetes")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation ("jakarta.validation:jakarta.validation-api:3.0.2")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    implementation(fileTree("lib") { include("*.jar") })
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation ("io.quarkus:quarkus-core")


    //TEMP - included for TAP dependencies - make Vollt tap stuff FAT JARs
    implementation ("uk.ac.starlink:stil:4.3") {
        //Transitive dependency (outdated) from starlink causes conflict with Vollt's TAP ADQL query for JSON output.
        exclude("org.json", "json")
    }
    implementation("org.json:json:20240303")

}

group = "org.uksrc.archive"
version = "0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
