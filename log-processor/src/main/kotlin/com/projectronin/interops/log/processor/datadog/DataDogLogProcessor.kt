package com.projectronin.interops.log.processor.datadog

import org.apache.commons.csv.CSVFormat
import org.apache.commons.lang3.StringUtils

abstract class DataDogLogProcessor {
    protected fun getLogEntries(csvFile: String): List<LogEntry> {
        val stream = javaClass.getResourceAsStream(csvFile)!!

        return CSVFormat.Builder.create().build().parse(stream.reader()).drop(1).mapNotNull {
            val date = it[0]
            val host = StringUtils.strip(it[1], "\"")
            val service = StringUtils.strip(it[2], "\"")
            val message = it[3]

            val level =
                when (message.split(" ")[0]) {
                    "ERROR" -> LogLevel.ERROR
                    "WARN" -> LogLevel.WARN
                    "INFO" -> LogLevel.INFO
                    "DEBUG" -> LogLevel.DEBUG
                    "TRACE" -> LogLevel.TRACE
                    else -> null
                }

            LogEntry(date, host, service, message, level)
        }
    }
}
