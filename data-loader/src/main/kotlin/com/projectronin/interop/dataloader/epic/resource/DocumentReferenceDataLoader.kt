package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class DocumentReferenceDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val documentReferenceService = EpicDocumentReferenceService(epicClient)

    // TODO: Move this to tenant when/if it becomes real
    private val clientNoteTypeSystem = "urn:oid:1.2.840.114350.1.13.297.2.7.4.737880.5010"

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, filename: String = "documentReferences.csv") {
        logger.info { "Loading observations" }
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""LOINC Note Type","Client Note Type","Escaped JSON"""")
            writer.newLine()
            var totalTime: Long = 0
            patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                val executionTime = measureTimeMillis {
                    val run = runCatching {
                        loadAndWriteDocumentReferences(patient, tenant, writer)
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
        logger.info { "Done loading document references" }
    }

    private fun loadAndWriteDocumentReferences(
        patient: Patient,
        tenant: Tenant,
        writer: BufferedWriter
    ) {
        getDocumentReferencesForPatient(patient, tenant).forEach { (data, docRef) ->
            writeDocumentReference(data, docRef, writer)
            writer.flush()
        }
    }

    private fun getDocumentReferencesForPatient(
        patient: Patient,
        tenant: Tenant
    ): List<Pair<DocumentReferenceData, DocumentReference>> =
        documentReferenceService.getDocumentReferencesByPatientAndDate(tenant, patient.id!!.value!!)
            .map { documentReference ->
                val loincNoteType =
                    documentReference.type?.coding?.firstOrNull { it.system?.value == "http://loinc.org" }?.display?.value
                val clientNoteType =
                    documentReference.type?.coding?.firstOrNull { it.system?.value == clientNoteTypeSystem }?.display?.value
                DocumentReferenceData(loincNoteType, clientNoteType, null) to documentReference
            }

    private fun writeDocumentReference(
        data: DocumentReferenceData,
        documentReference: DocumentReference,
        writer: BufferedWriter
    ) {
        val json = objectMapper.writeValueAsString(documentReference)
        val escapedJson = StringEscapeUtils.escapeCsv(json)
        writer.write(""""${data.loincNoteType}","${data.clientNoteType}",$escapedJson""")
        writer.newLine()
    }
}

data class DocumentReferenceData(
    val loincNoteType: String?,
    val clientNoteType: String?,
    val category: String?
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

    fun getDocumentReferencesByPatientAndDate(
        tenant: Tenant,
        patientFHIRId: String,
    ): List<DocumentReference> {
        val parameters = mapOf("patient" to patientFHIRId, "date" to "ge2022-12-28")
        return getResourceListFromSearch(tenant, parameters)
    }
}
