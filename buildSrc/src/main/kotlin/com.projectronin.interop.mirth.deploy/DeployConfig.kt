package com.projectronin.interop.mirth.deploy

class DeployConfig {
    var name: String = ""
}

fun deployConfig(block: DeployConfig.() -> Unit) {
    val config = DeployConfig().apply(block)
    MirthDeployTask.registerDeployConfig(config)
}
