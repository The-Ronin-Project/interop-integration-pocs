plugins {
    `kotlin-dsl`
    `java-gradle-plugin`

    id("com.projectronin.interop.gradle.base")
}

gradlePlugin {
    plugins {
        create("mirthDeployPlugin") {
            id = "com.projectronin.interop.mirth.deploy"
            implementationClass = "com.projectronin.interop.mirth.deploy.MirthDeployGradlePlugin"
        }
    }
}
