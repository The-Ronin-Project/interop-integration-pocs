package com.projectronin.interop.mirth.deploy

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class MirthDeployTask : DefaultTask() {
    companion object {
        var config: DeployConfig = DeployConfig()
        private var configSet: Boolean = false

        fun registerDeployConfig(deployConfig: DeployConfig) {
            if (configSet) {
                throw IllegalStateException("Deploy Config has already been set")
            }
            config = deployConfig
            configSet = true
        }
    }

    @TaskAction
    fun deploy() {
        logger.lifecycle(config.name)
    }
}
