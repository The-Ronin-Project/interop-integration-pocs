plugins {
    id("com.projectronin.interop.gradle.base")
    id("org.openapi.generator") version "6.0.1"
    id("org.springframework.boot") version "2.7.2"
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-parent:2.7.2"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.8")

    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("javax.servlet:javax.servlet-api")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$buildDir/generated")
    ignoreFileOverride.set("$projectDir/.openapi-generator-ignore")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "packageName" to "com.projectronin.interop.openapi",
            "basePackage" to "com.projectronin.interop.openapi",
        )
    )
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate.get())
}

kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated/src/main/kotlin")
