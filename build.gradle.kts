plugins {
    kotlin("jvm") version "1.8.0"
    `kotlin-dsl`
    jacoco
    id("pl.allegro.tech.build.axion-release") version "1.14.4"
    id("com.projectronin.interop.gradle.base") version "3.0.0"

    alias(libs.plugins.interop.spring.boot) apply false
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
        tag {
            initialVersion(pl.allegro.tech.build.axion.release.domain.properties.TagProperties.InitialVersionSupplier { _, _ -> "1.0.0" })
            prefix.set("")
        }
        versionCreator { versionFromTag, position ->
            val supportedHeads = setOf("HEAD", "master", "main")
            if (!supportedHeads.contains(position.branch)) {
                val jiraBranchRegex = Regex("(\\w+)-(\\d+)-(.+)")
                val match = jiraBranchRegex.matchEntire(position.branch)
                val branchExtension =
                    match?.let {
                        val (project, number, _) = it.destructured
                        "$project$number"
                    } ?: position.branch

                "$versionFromTag-$branchExtension"
            } else {
                versionFromTag
            }
        }
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

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.base")

    // Disable releases hub from running on the subprojects. Main project will handle it all.
    tasks.filter { it.group.equals("releases hub", ignoreCase = true) }.forEach { it.enabled = false }
}
