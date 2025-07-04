
plugins {
    java
    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

group = "org.uksrc.archive"
version = "0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("io.quarkus:quarkus-container-image-docker")
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation ("io.quarkus:quarkus-core")
    implementation("io.quarkus:quarkus-undertow")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-resteasy-reactive-jaxb")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-kubernetes")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-arc")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    implementation ("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("org.javastro:jaxbjpa-utils:0.2.3")
    implementation("io.quarkus:quarkus-agroal")
    implementation("commons-beanutils:commons-beanutils:1.9.4")

    //Vollt TAP
    implementation("fr.unistra.cds:ADQLlib:2.0-SNAPSHOT")
    implementation("fr.unistra.cds:TAPlib:2.4.3-SNAPSHOT")
    implementation("fr.unistra.cds:UWSlib:4.4-SNAPSHOT")

    implementation("org.javastro.ivoa.dm:tapschema:0.9.4")


    //Model(s)
    implementation("org.opencadc:CAOM:2.5.6-SNAPSHOT:quarkus")

    //Required by Vollt
    // TAP lib upload func.
    implementation("org.apache.commons:commons-fileupload2-javax:2.0.0-M2")
    implementation("org.apache.commons:commons-fileupload2-jakarta-servlet6:2.0.0-M2")

    implementation ("uk.ac.starlink:stil:4.3.1")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

apply(from = "src/main/kotlin/generateVolltWebXml.gradle.kts")

tasks.named("build") {
    dependsOn("generateVolltWebXml")
}

tasks.named("quarkusDev") {
    dependsOn("generateVolltWebXml")
}


