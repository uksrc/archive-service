
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
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-jaxb")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-kubernetes")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-arc")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    implementation ("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("org.javastro:jaxbjpa-utils:0.2.3")
    implementation("io.quarkus:quarkus-agroal")
    implementation("commons-beanutils:commons-beanutils:1.11.0")
    implementation("io.quarkus:quarkus-kubernetes-config")
    implementation("org.json:json:20250517")


    //Model(s)
    implementation("org.javastro.ivoa.dm:tapschema:0.9.7")
    implementation("org.opencadc:CAOM:2.5.7-SNAPSHOT:quarkus")

    implementation ("uk.ac.starlink:stil:4.3.1")

    //Identity Management
    implementation("io.quarkus:quarkus-oidc")

    //Basic Auth
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-security-jpa")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-elytron-security-properties-file")

    //Datalink
    implementation("org.apache.tika:tika-core:3.2.2")

    //UserAgent evaluation
    implementation("nl.basjes.parse.useragent:yauaa:7.31.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-core:2.25.4")

    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("org.javastro:jsofa:20210512")

    //Tap service
    implementation("org.javastro.ivoa.core:tap:0.9.0")
    implementation("org.javastro.ivoa.core:dal:0.9.0")
    implementation("org.javastro.ivoa.core:pgsphere:0.9.1")
    implementation("org.javastro.ivoa.core.quarkus:quarkus-tap-lib:0.9.0")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
   // testLogging.showStandardStreams = true
    useJUnitPlatform()
    systemProperty("quarkus.profile", "test")
}

sourceSets {
    named("main") {
        resources {
            srcDir("build/generated-resources")
        }
    }
}


// Optionally ensure this all happens before compile/resources
/*tasks.named("classes") {
    dependsOn("generateTapProperties", "generateWebXml")
}*/



