package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.service.GetPatientStagingResponse
import com.projectronin.interop.dataloader.epic.service.StagingReportService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

@Suppress("ktlint:standard:max-line-length")
class StagingDataLoader(epicClient: EpicClient) : BaseEpicDataLoader() {
    private val stagingReportService = StagingReportService(epicClient)
    override val jira = "Prior to paradigm change"

    override fun main() = TODO("Prior to paradigm change")

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        filename: String = "staging.csv",
    ): Map<String, Patient> {
        logger.info { "Loading staging reports" }

        val cancerPatientsByMrn = mutableMapOf<String, Patient>()

        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(
                listOf(
                    "ID",
                    "MRN",
                    "Problem Description",
                    "Problem ID",
                    "Staging Method",
                    "Classification",
                    "TValue",
                    "NValue",
                    "MValue",
                    "Stage Group",
                    "Stage Modifier",
                    "ER Status",
                    "PR Status",
                    "HER Status",
                    "Stage Date",
                    "Diagnosis Date",
                    "Diagnosis Code Set",
                    "Diagnosis Code",
                    "Histologic Grade",
                    "Histologic System",
                    "Histologic Method",
                    "JSON",
                ).joinToString(",") { "\"$it\"" },
            )
            writer.newLine()

            var totalTime: Long = 0
            patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                val executionTime =
                    measureTimeMillis {
                        val run =
                            runCatching {
                                if (loadAndWriteStagingReports(tenant, mrn, patient, writer)) {
                                    cancerPatientsByMrn[mrn] = patient
                                }
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

        logger.info { "Done loading staging reports" }
        return cancerPatientsByMrn
    }

    private fun loadAndWriteStagingReports(
        tenant: Tenant,
        mrn: String,
        patient: Patient,
        writer: BufferedWriter,
    ): Boolean {
        val stagingReport = stagingReportService.getStagingReportsByPatient(tenant, mrn)
        writeStagingReport(stagingReport, mrn, patient, writer)
        writer.flush()

        // Return true if the patient has staging data
        return stagingReport.stages.isNotEmpty()
    }

    private fun writeStagingReport(
        stagingReport: GetPatientStagingResponse,
        mrn: String,
        patient: Patient,
        writer: BufferedWriter,
    ) {
        val json = JacksonManager.objectMapper.writeValueAsString(stagingReport)
        val escapedJson = StringEscapeUtils.escapeCsv(json)

        if (stagingReport.stages.isEmpty()) {
            writer.write("\"${patient.id!!.value!!}\",\"$mrn\"" + ",\"\"".repeat(19) + ",$escapedJson")
            writer.newLine()
        } else {
            stagingReport.stages.forEach { stage ->
                writer.write(
                    listOf(
                        patient.id!!.value!!,
                        mrn,
                        stage.problemDescription,
                        stage.problemID,
                        stage.stagingMethod,
                        stage.classification,
                        stage.tValue,
                        stage.nValue,
                        stage.mValue,
                        stage.stageGroup,
                        stage.stageModifier,
                        stage.eRStatus,
                        stage.pRStatus,
                        stage.hER2Status,
                        stage.stageDate,
                        stage.diagnosisDate,
                        stage.diagnosisCodeSet,
                        stage.diagnosisCode,
                        stage.histologicGrade?.grade,
                        stage.histologicGrade?.system,
                        stage.histologicGrade?.method,
                    ).joinToString(",") { "\"${it.orEmpty()}\"" } + ",$escapedJson",
                )
                writer.newLine()
            }
        }
    }
}
