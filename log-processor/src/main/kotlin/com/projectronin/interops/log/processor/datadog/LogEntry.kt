package com.projectronin.interops.log.processor.datadog

data class LogEntry(
    val date: String,
    val host: String,
    val service: String,
    val message: String,
    val level: LogLevel?,
)

enum class LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
}
