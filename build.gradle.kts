plugins {
    kotlin("jvm") version "1.6.10"
    `kotlin-dsl`
    jacoco
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("pl.allegro.tech.build.axion-release") version "1.13.6"
}

repositories {
    maven {
        url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
        mavenContent {
            releasesOnly()
        }
    }
    maven(url = "https://plugins.gradle.org/m2/")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "pl.allegro.tech.build.axion-release")

// Java/Kotlin versioning
    java {
        sourceCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }

// Versioning/release
    scmVersion {
        tag(
            closureOf<pl.allegro.tech.build.axion.release.domain.TagNameSerializationConfig> {
                initialVersion =
                    org.gradle.kotlin.dsl.KotlinClosure2<pl.allegro.tech.build.axion.release.domain.properties.TagProperties, pl.allegro.tech.build.axion.release.domain.scm.ScmPosition, String>(
                        { _, _ -> "1.0.0" }
                    )
                prefix = ""
            }
        )
        versionCreator =
            KotlinClosure2<String, pl.allegro.tech.build.axion.release.domain.scm.ScmPosition, String>({ versionFromTag, position ->
                if (position.branch != "main" && position.branch != "HEAD") {
                    val jiraBranchRegex = Regex("(\\w+)-(\\d+)-(.+)")
                    val match = jiraBranchRegex.matchEntire(position.branch)

                    val branchExtension = match?.let {
                        val (project, number, _) = it.destructured
                        "$project$number"
                    } ?: position.branch

                    "$versionFromTag-$branchExtension"
                } else {
                    versionFromTag
                }
            })
    }

    project.version = scmVersion.version

    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        maven(url = "https://plugins.gradle.org/m2/")
    }

// Setup Jacoco for the tests
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    jacoco {
        toolVersion = "0.8.7"
        // Custom reports directory can be specfied like this:
        reportsDirectory.set(file("./codecov"))
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.required.set(true)
        }
    }

    tasks {
        test {
            testLogging.showStandardStreams = true
            testLogging.showExceptions = true
        }
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

// ktlint includes the generated-sources, which includes the classes created by Gradle for these plugins
    ktlint {
        enableExperimentalRules.set(true)
        filter {
            // We should be able to just do a wildcard exclude, but it's not working.
            // This solution comes from https://github.com/JLLeitschuh/ktlint-gradle/issues/222#issuecomment-480758375
            exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated-sources/") }
        }
    }
}
