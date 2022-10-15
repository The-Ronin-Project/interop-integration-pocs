package com.projectronin.interops.log.processor.validation

import com.projectronin.interops.log.processor.datadog.DataDogLogProcessor
import com.projectronin.interops.log.processor.datadog.LogEntry

fun main() {
    ValidationLogProcessor().process()
}

class ValidationLogProcessor : DataDogLogProcessor() {
    private val validationLogIndicator = "Encountered validation error(s)"

    fun process() {
        val file = "/logs.csv"
        val entries = getLogEntries(file)

        // DataDog alerts show up in the logs, but they have no level, so lets remove them
        val leveledEntries = entries.filterNot { it.level == null }

        val validationEntries = entries.filter { it.message.contains(validationLogIndicator) }
        println("Total validation entires: ${validationEntries.size}")
        val validationEntriesByType = getValidationIssues(validationEntries)
        validationEntriesByType.forEach {
            println("${it.value.size}: ${it.key}")
        }
        println()

        val nonValidationEntries = leveledEntries - validationEntries
        println("Total non-validation entires: ${nonValidationEntries.size}")
        val nonValidationEntriesByType = getNonValidationIssues(nonValidationEntries)
        nonValidationEntriesByType.forEach {
            println("${it.value.size}: ${it.key}")
        }
    }

    private fun getValidationIssues(entries: List<LogEntry>): Map<String, List<LogEntry>> {
        return entries.groupBy {
            val firstIndex = it.message.indexOf(validationLogIndicator) + validationLogIndicator.length + 1
            val secondIndex = it.message.indexOf("at com.", startIndex = firstIndex)
            it.message.substring(firstIndex, secondIndex).trim()
        }
    }

    private fun getNonValidationIssues(entries: List<LogEntry>): Map<String, List<LogEntry>> {
        return entries.groupBy {
            // The first 3 portions indicate the level, date the time

            it.message.split(" ").drop(3)
                // We also drop the thread portion so we get a clean reference
                .filterNot { l -> l.contains("-thread-") }.joinToString(" ")
        }
    }
}
