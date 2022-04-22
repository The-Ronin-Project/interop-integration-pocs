dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.6.21"))

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:30.1-jre")

    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:5.7.2")

    implementation("ca.uhn.hapi:hapi-structures-v281:2.3")

    implementation("org.liquibase:liquibase-core:4.9.1")

    implementation("org.springframework.boot:spring-boot-starter-jdbc:2.6.7")

    testImplementation("org.testcontainers:junit-jupiter:1.17.1")
    testImplementation("org.testcontainers:postgresql:1.17.1")
    testRuntimeOnly("org.postgresql:postgresql:42.2.18")
    testImplementation("org.ktorm:ktorm-core:3.4.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testRuntimeOnly("org.yaml:snakeyaml:1.30")
    testImplementation("com.github.database-rider:rider-junit5:1.32.3")
    testImplementation("io.mockk:mockk:1.12.3")
}

// Publishing
publishing {
    repositories {
        maven {
            name = "nexus"
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_TOKEN")
            }
            url = if (project.version.toString().endsWith("SNAPSHOT")) {
                uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            } else {
                uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

tasks.register("install") {
    dependsOn(tasks.publishToMavenLocal)
}
