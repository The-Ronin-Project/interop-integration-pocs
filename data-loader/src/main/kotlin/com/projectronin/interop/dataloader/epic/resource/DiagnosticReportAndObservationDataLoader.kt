package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

/*
    This should probably be combined with the [DiagnosticReportDataLoader], but this is a one-off so this is good
    enough to get the data we need.

    I originally used the _include parameter to load observations tied to the DxReport, but it was timing out too
    often, so switched to pulling them in separate queries.  This makes things take MUCH longer, but it works.  There
    aren't any more failures, but we average about 8 minutes per patient.
 */
class DiagnosticReportAndObservationDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val diagnosticReportService = EpicDiagnosticReportObservationService(epicClient)
    private val observationService = EpicObservationServiceBundle(epicClient)

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant
    ) {
        logger.info { "Loading diagnostic reports" }
        var totalTime: Long = 0
        patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
            val executionTime = measureTimeMillis {
                val run = runCatching {
                    loadAndWriteDiagnosticReports(patient, tenant, mrn)
                }

                if (run.isFailure) {
                    val exception = run.exceptionOrNull()
                    logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
                }
            }

            totalTime += executionTime
            logger.info { "Completed ${index + 1} of ${patientsByMrn.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
        }
        logger.info { "Done loading diagnostic reports" }
    }

    private fun loadAndWriteDiagnosticReports(
        patient: Patient,
        tenant: Tenant,
        mrn: String
    ) {
        logger.info { "Getting DxReports for patient $mrn" }
        val bundle = diagnosticReportService.getDiagnosticReportsByPatient(tenant, patient.id!!.value!!)
        val dxReportsCount = bundle.entry.mapNotNull { it.resource }.filterIsInstance(DiagnosticReport::class.java).count()

        val observationBundles = mutableListOf<Bundle>()

        bundle.entry
            .mapNotNull { it.resource }
            .filterIsInstance(DiagnosticReport::class.java)
            .forEachIndexed { index, dxReport ->
                val observationCount = dxReport.result.count { it.decomposedType() == "Observation" }
                logger.info { "Getting $observationCount observations for DxReport ${dxReport.id!!.value} ($index of $dxReportsCount)" }

                dxReport.result.filter { it.decomposedType() == "Observation" }.forEach { reference ->
                    observationBundles.add(
                        observationService.getObservationBundle(tenant, reference.decomposedId()!!)
                    )
                }
            }

        val json = JacksonManager.objectMapper.writeValueAsString(
            observationService.mergeResponses(listOf(bundle) + observationBundles)
        )

        BufferedWriter(FileWriter(File("loaded/$mrn.json"))).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }
}

class EpicDiagnosticReportObservationService(epicClient: EpicClient) : BaseEpicService<Bundle>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DiagnosticReport"
    override val fhirResourceType = Bundle::class.java

    fun getDiagnosticReportsByPatient(tenant: Tenant, patientFhirId: String): Bundle {
        val parameters = mapOf(
            "patient" to patientFhirId
        )
        return getBundleWithPaging(tenant, parameters)
    }
}

class EpicObservationServiceBundle(epicClient: EpicClient) : BaseEpicService<Bundle>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Observation"
    override val fhirResourceType = Bundle::class.java

    fun getObservationBundle(tenant: Tenant, observationFhirId: String): Bundle = searchByID(tenant, observationFhirId)
}
