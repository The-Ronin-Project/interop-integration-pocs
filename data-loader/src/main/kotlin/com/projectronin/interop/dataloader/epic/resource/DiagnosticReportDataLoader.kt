package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class DiagnosticReportDataLoader(
    epicClient: EpicClient,
    authenticationBroker: EHRAuthenticationBroker,
    httpClient: HttpClient
) {
    private val logger = KotlinLogging.logger { }
    private val diagnosticReportService = EpicDiagnosticReportService(epicClient)
    private val binaryService = EpicBinaryService(epicClient, authenticationBroker, httpClient)

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

class EpicBinaryService(
    private val epicClient: EpicClient,
    private val authenticationBroker: EHRAuthenticationBroker,
    private val client: HttpClient
) {
    private val logger = KotlinLogging.logger { }

    fun getBinaryData(tenant: Tenant, binaryFhirId: String): String {
        return runBlocking { epicClient.get(tenant, "/api/FHIR/R4/Binary/$binaryFhirId", emptyMap()).bodyAsText() }
    }

    fun getBinary(tenant: Tenant, binaryFhirId: String): Binary? {
        return get(tenant, "/api/FHIR/R4/Binary/$binaryFhirId", emptyMap())
    }

    /**
     * Stolen from EpicClient and adjusted to use the FHIR JSON ContentType.
     */
    fun get(tenant: Tenant, urlPart: String, parameters: Map<String, Any?> = mapOf()): Binary? {
        // Authenticate
        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")

        val requestUrl =
            if (urlPart.first() == '/') {
                tenant.vendor.serviceEndpoint + urlPart
            } else {
                urlPart
            }

        val resource: Resource<*> = runBlocking {
            val response: HttpResponse = client.get(requestUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.FhirJson)
                parameters.map {
                    val key = it.key
                    val value = it.value
                    if (value is List<*>) {
                        value.forEach { repetition ->
                            parameter(key, repetition)
                        }
                    } else {
                        value?.let { parameter(key, value) }
                    }
                }
            }

            response.body()
        }

        return if (resource is Binary) {
            resource
        } else {
            logger.warn { "Could not load Binary $urlPart: $resource" }
            null
        }
    }
}
