package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.time.ExperimentalTime

class DocumentReferenceDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val documentReferenceService = EpicDocumentReferenceService(epicClient)

    // TODO: Move this to tenant when/if it becomes real
    private val clientNoteTypeSystem = "urn:oid:1.2.840.114350.1.13.297.2.7.4.737880.5010"

    @OptIn(ExperimentalTime::class)
    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, filename: String = "documentReferences.csv") {
        logger.info { "Loading document references" }

        val allReferences = patientsByMrn.entries.flatMapIndexed { index, (mrn, patient) ->
            val run = runCatching {
                getDocumentReferencesForPatient(patient, tenant, "clinical-note") + getDocumentReferencesForPatient(
                    patient,
                    tenant,
                    "imaging-result"
                )
            }

            if (run.isFailure) {
                val exception = run.exceptionOrNull()
                logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
            }

            logger.info { "Completed ${index + 1} of ${patientsByMrn.size}" }
            run.getOrDefault(emptyList())
        }
        val uniqueReferences = allReferences.toSet()
        logger.info { "Found ${allReferences.size} distinct document references. ${uniqueReferences.size} contained unique note types and categories" }

        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""LOINC Note Type","Client Note Type","Category"""")
            writer.newLine()

            uniqueReferences.groupBy { it.category }.forEach {
                it.value.forEach {
                    writer.write(""""${it.loincNoteType}","${it.clientNoteType}","${it.category}"""")
                    writer.newLine()
                }
            }

        }
        logger.info { "Done loading document references" }
    }

    private fun getDocumentReferencesForPatient(
        patient: Patient,
        tenant: Tenant,
        category: String
    ): List<DocumentReferenceData> =
        documentReferenceService.getDocumentReferencesByPatientAndCategory(tenant, patient.id!!.value!!, category)
            .map { documentReference ->
                val loincNoteType =
                    documentReference.type?.coding?.firstOrNull { it.system?.value == "http://loinc.org" }?.display?.value
                val clientNoteType =
                    documentReference.type?.coding?.firstOrNull { it.system?.value == clientNoteTypeSystem }?.display?.value
                DocumentReferenceData(loincNoteType, clientNoteType, category)
            }
}

data class DocumentReferenceData(
    val loincNoteType: String?,
    val clientNoteType: String?,
    val category: String
)

class EpicDocumentReferenceService(epicClient: EpicClient) :
    BaseEpicService<DocumentReference>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DocumentReference"
    override val fhirResourceType = DocumentReference::class.java

    fun getDocumentReferencesByPatientAndCategory(
        tenant: Tenant,
        patientFHIRId: String,
        category: String
    ): List<DocumentReference> {
        val parameters = mapOf("patient" to patientFHIRId, "category" to category)
        return getResourceListFromSearch(tenant, parameters)
    }
}
