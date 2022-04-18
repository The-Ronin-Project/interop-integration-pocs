package com.projectronin.integration.demo

class StaticClass {
    companion object {
        private var count = 1
        private var admitService = null
    }

    fun getCount(): Int = StaticClass.count++

    fun admitService(): String? = admitService
}
