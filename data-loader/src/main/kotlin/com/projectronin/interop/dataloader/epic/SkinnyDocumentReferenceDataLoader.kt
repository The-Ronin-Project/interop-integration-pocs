package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.service.DocumentReferenceService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Attempts to load all of the patients document references without following the references and actually pulling
 * in the documents.  Writes them to a file named "mrn-docreferences.json"
 */
class SkinnyDocumentReferenceDataLoader(epicClient: EpicClient) : BaseEpicDataLoader() {
    private val epicDocumentReferenceService = DocumentReferenceService(epicClient)

    private val clinicalNoteCategory = "clinical-note"
    private val radiologyResultCategory = "imaging-result"
    override val jira = "Prior to paradigm change"

    override fun main() = TODO("Prior to paradigm change")

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
    ) {
        logger.info { "Loading document references" }

        patientsByMrn.map { (mrn, patient) ->
            logger.info { "Getting doc reference for MRN $mrn" }

            val documentReferences =
                (
                    epicDocumentReferenceService.getDocumentReferences(tenant, patient.id!!.value!!, clinicalNoteCategory) +
                        epicDocumentReferenceService.getDocumentReferences(tenant, patient.id!!.value!!, radiologyResultCategory)
                ).toSet()

            if (documentReferences.isNotEmpty()) {
                BufferedWriter(FileWriter(File("loaded/$mrn-docreferences.json"))).use { writer ->
                    writer.write(
                        JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(documentReferences),
                    )
                }
            }
        }

        logger.info { "Done loading document references" }
    }
}
