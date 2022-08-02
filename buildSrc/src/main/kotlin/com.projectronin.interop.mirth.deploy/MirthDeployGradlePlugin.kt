package com.projectronin.interop.mirth.deploy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class MirthDeployGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<MirthDeployTask>("mirthDeploy")
    }
}
