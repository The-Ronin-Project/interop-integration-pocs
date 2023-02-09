package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.Code
import com.projectronin.interop.ehr.epic.EpicConditionService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class ConditionDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val flushFrequency = 1_000
    private val conditionService = EpicConditionService(epicClient)

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, filename: String = "conditions.csv") {
        logger.info { "Loading conditions" }
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""MRN","Condition FHIR ID","Category","Code System","Code","Display","Escaped JSON"""")
            writer.newLine()

            var totalTime: Long = 0
            patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                val executionTime = measureTimeMillis {
                    val run = runCatching {
                        loadAndWriteConditions(patient, tenant, "encounter-diagnosis", mrn, writer)
                        loadAndWriteConditions(patient, tenant, "problem-list-item", mrn, writer)
                    }

                    if (run.isFailure) {
                        val exception = run.exceptionOrNull()
                        logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
                    }
                }

                totalTime += executionTime
                logger.info { "Completed ${index + 1} of ${patientsByMrn.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
            }
        }
        logger.info { "Done loading conditions" }
    }

    private fun loadAndWriteConditions(
        patient: Patient,
        tenant: Tenant,
        category: String,
        mrn: String,
        writer: BufferedWriter
    ) {
        getConditionsForPatient(patient, tenant, category).entries.forEachIndexed { index, (key, value) ->
            writeCondition(key, value, category, mrn, writer)

            if (index % flushFrequency == 0) {
                writer.flush()
            }
        }
    }

    private fun getConditionsForPatient(patient: Patient, tenant: Tenant, category: String): Map<Code, Condition> =
        conditionService.findConditions(tenant, patient.id!!.value!!, category, "active").mapNotNull {
            it.code?.coding?.map { coding ->
                Pair(
                    Code(
                        coding.system?.value ?: "",
                        coding.code?.value ?: "",
                        coding.display?.value ?: ""
                    ),
                    it
                )
            }
        }.flatten().associate { it.first to it.second }

    private fun writeCondition(
        code: Code,
        condition: Condition,
        category: String,
        mrn: String,
        writer: BufferedWriter
    ) {
        val json = JacksonManager.objectMapper.writeValueAsString(condition)
        val escapedJson = StringEscapeUtils.escapeCsv(json)
        writer.write(""""$mrn","${condition.id!!.value}","$category","${code.system}","${code.code}","${code.display}",$escapedJson""")
        writer.newLine()
    }
}
