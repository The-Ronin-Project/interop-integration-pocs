package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.BaseEpicService
import com.projectronin.interop.dataloader.epic.service.BinaryService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class DiagnosticReportDataLoader(
    epicClient: EpicClient,
    authenticationBroker: EHRAuthenticationBroker,
    httpClient: HttpClient
) : BaseEpicDataLoader() {
    private val diagnosticReportService = EpicDiagnosticReportService(epicClient)
    private val binaryService = BinaryService(epicClient, authenticationBroker, httpClient)
    override val jira = "Prior to paradigm change"
    override fun main() = TODO("Prior to paradigm change")

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, filename: String = "diagnostics.csv") {
        logger.info { "Loading diagnostic reports" }
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""MRN","Diagnostic Report ID","Effective Date","Code Type","Status","Title","Content Type","Content"""")
            writer.newLine()

            var totalTime: Long = 0
            patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                val executionTime = measureTimeMillis {
                    val run = runCatching {
                        loadAndWriteDiagnosticReports(patient, tenant, mrn, writer)
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
        logger.info { "Done loading diagnostic reports" }
    }

    private fun loadAndWriteDiagnosticReports(
        patient: Patient,
        tenant: Tenant,
        mrn: String,
        writer: BufferedWriter
    ) {
        getDiagnosticReportsForPatient(patient, tenant).forEachIndexed { index, diagnosticReport ->
            writeDiagnosticReport(diagnosticReport, mrn, tenant, writer)
            writer.flush()
        }
    }

    private fun getDiagnosticReportsForPatient(patient: Patient, tenant: Tenant): List<DiagnosticReport> =
        diagnosticReportService.getDiagnosticReportsByPatient(tenant, patient.id!!.value!!)

    private fun writeDiagnosticReport(
        diagnosticReport: DiagnosticReport,
        mrn: String,
        tenant: Tenant,
        writer: BufferedWriter
    ) {
        val date = diagnosticReport.effective?.let {
            when (it.type) {
                DynamicValueType.DATE_TIME -> (it.value as DateTime).value
                DynamicValueType.PERIOD -> {
                    val period = (it.value as Period)
                    "${period.start?.value} - ${period.end?.value}"
                }

                else -> null
            }
        }
        diagnosticReport.presentedForm.forEach { attachment ->
            val binaryFhirId = attachment.url?.value?.removePrefix("Binary/")
            if (binaryFhirId == null) {
                logger.error { "No Binary ID found for $attachment" }
                return@forEach
            }
            val binary = binaryService.getBinaryData(tenant, binaryFhirId)
            val escapedBinary = StringEscapeUtils.escapeCsv(binary)
            writer.write(""""$mrn","${diagnosticReport.id!!.value}","$date","${diagnosticReport.code?.text?.value}","${diagnosticReport.status?.value}","${attachment.title?.value}","${attachment.contentType?.value}",$escapedBinary""")
            writer.newLine()
        }
    }
}

class EpicDiagnosticReportService(epicClient: EpicClient) : BaseEpicService<DiagnosticReport>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DiagnosticReport"
    override val fhirResourceType = DiagnosticReport::class.java

    fun getDiagnosticReportsByPatient(tenant: Tenant, patientFhirId: String): List<DiagnosticReport> {
        val parameters = mapOf("patient" to patientFhirId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
