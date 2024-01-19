package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.service.DiagnosticReportService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Attempts to load all of the patients Dx reports without following references and pulling in documents.
 * Writes them to a file named "diagnostic_report_mrn.json" and uploads them to the OCI experimentation bucket
 * under the given timestamp.
 */
class SkinnyDiagnosticReportDataLoader(epicClient: EpicClient) : BaseEpicDataLoader() {
    private val epicDiagnosticReportService = DiagnosticReportService(epicClient)
    override val jira = "Prior to paradigm change"

    override fun main() = TODO("Prior to paradigm change")

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        timeStamp: String,
    ) {
        logger.info { "Loading dx reports" }

        patientsByMrn.entries.mapIndexed { index, (mrn, patient) ->
            logger.info { "Getting reports for MRN $mrn (${index + 1} of ${patientsByMrn.size})" }
            val fileName = "loaded/diagnostic_report_$mrn.json"

            val dxReports = epicDiagnosticReportService.getDiagnosticReportsByPatient(tenant, patient.id!!.value!!)

            if (dxReports.isNotEmpty()) {
                BufferedWriter(FileWriter(File(fileName))).use { writer ->
                    writer.write(
                        JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dxReports),
                    )
                }

                logger.info { "Writing reports for MRN $mrn" }
                expOCIClient.uploadExport(tenant, "diagnostic_report", fileName, timeStamp)
            }
        }

        logger.info { "Done loading dx reports" }
    }
}
