package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Attempts to load all of the patients Dx reports without following references and pulling in documents.
 * Writes them to a file named "mrn-dxreports.json"
 */
class SkinnyDiagnosticReportDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val epicDiagnosticReportService = EpicDiagnosticReportService(epicClient)

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant) {
        logger.info { "Loading dx reports" }

        patientsByMrn.map { (mrn, patient) ->
            logger.info { "Getting report for MRN $mrn" }
            val dxReports = epicDiagnosticReportService.getDiagnosticReportsByPatient(tenant, patient.id!!.value!!)

            if (dxReports.isNotEmpty()) {
                BufferedWriter(FileWriter(File("loaded/$mrn-dxreports.json"))).use { writer ->
                    writer.write(
                        JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(dxReports)
                    )
                }
            }
        }

        logger.info { "Done loading dx reports" }
    }
}
