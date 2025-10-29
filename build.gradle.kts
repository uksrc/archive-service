
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
    implementation("io.quarkus:quarkus-kubernetes-config")

    //TAP
    implementation("fr.unistra.cds:ADQLlib:2.0-SNAPSHOT")
    implementation("org.javastro.ivoa.dm:tapschema:0.9.5")

    //Model(s)
    implementation("org.opencadc:CAOM:2.5.6-SNAPSHOT:quarkus")

    implementation ("uk.ac.starlink:stil:4.3.1")

    //Identity Management
    implementation("io.quarkus:quarkus-oidc")

    //Datalink
    implementation("org.apache.tika:tika-core:3.2.2")

    //UserAgent evaluation
    implementation("nl.basjes.parse.useragent:yauaa:7.31.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")

    testImplementation("io.quarkus:quarkus-test-security")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
