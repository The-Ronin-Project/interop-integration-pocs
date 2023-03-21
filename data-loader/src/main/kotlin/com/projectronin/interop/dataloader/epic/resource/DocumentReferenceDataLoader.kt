package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Paths
import java.util.Base64
import kotlin.io.path.createDirectories

class DocumentReferenceDataLoader(
    epicClient: EpicClient,
    authenticationBroker: EHRAuthenticationBroker,
    httpClient: HttpClient
) {
    private val logger = KotlinLogging.logger { }
    private val documentReferenceService = EpicDocumentReferenceService(epicClient)
    private val binaryService = EpicBinaryService(epicClient, authenticationBroker, httpClient)

    private val clinicalNoteCategory = "clinical-note"
    private val radiologyResultCategory = "imaging-result"

    private var totalAttachments = 0
    private var totalDatas = 0
    private var totalUrls = 0
    private var totalBinaries = 0
    private val contentTypeCounts = mutableMapOf<String, Int>()
    private var statusCounts = mutableMapOf<String, Int>()
    private var docStatusCounts = mutableMapOf<String, Int>()

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant) {
        logger.info { "Loading DocumentReferences" }
        val documentReferences = patientsByMrn.map { (_, patient) ->
            documentReferenceService.getDocumentReferences(tenant, patient.id!!.value!!, clinicalNoteCategory) +
                documentReferenceService.getDocumentReferences(tenant, patient.id!!.value!!, radiologyResultCategory)
        }.flatten().toSet()

        logger.info { "Writing ${documentReferences.size} document references to files." }

        runCatching { Paths.get("loaded/docRef/binary").createDirectories() }

        documentReferences.forEach { docRef ->
            increment(docRef.status!!.value!!, statusCounts)
            docRef.docStatus?.value?.let { increment(it, docStatusCounts) }

            docRef.content.forEachIndexed { index, content ->
                totalAttachments++

                val attachment = content.attachment!!
                attachment.data?.value?.let { totalDatas++ } ?: attachment.url?.value?.let {
                    totalUrls++
                    if (it.startsWith("Binary/")) {
                        totalBinaries++

                        when (attachment.contentType?.value) {
                            "text/plain" -> "txt"
                            "text/html" -> "html"
                            "application/pdf" -> "pdf"
                            else -> null
                        }?.let { extension ->
                            val file = File("loaded/docRef/binary/${docRef.id!!.value!!}-binary$index.$extension")
                            val binaryFhirId = it.removePrefix("Binary/")

                            if (extension == "pdf") {
                                binaryService.getBinary(tenant, binaryFhirId)?.let { binary ->
                                    val binaryBytes = Base64.getDecoder().decode(binary.data!!.value!!)
                                    FileOutputStream(file).use { writer ->
                                        writer.write(binaryBytes)
                                    }
                                }
                            } else {
                                val binary = binaryService.getBinaryData(tenant, binaryFhirId)
                                BufferedWriter(FileWriter(file)).use { writer ->
                                    writer.write(binary)
                                }
                            }
                        }
                    }
                }
                attachment.contentType?.value?.let { increment(it, contentTypeCounts) }
            }
            BufferedWriter(FileWriter(File("loaded/docRef/${docRef.id!!.value!!}.json"))).use { writer ->
                writer.write(JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(docRef))
            }
        }

        logger.info { "" }
        logger.info { "Status data: " }
        statusCounts.entries.sortedBy { it.value }.forEach { logger.info { "${it.key}: ${it.value}" } }

        logger.info { "" }
        logger.info { "Doc Status data: " }
        docStatusCounts.entries.sortedBy { it.value }.forEach { logger.info { "${it.key}: ${it.value}" } }

        logger.info { "" }
        logger.info { "Total attachments: $totalAttachments" }
        logger.info { "Total data elements: $totalDatas" }
        logger.info { "Total URLs: $totalUrls" }
        logger.info { "Total Binaries: $totalUrls" }

        logger.info { "" }
        logger.info { "Content-type data: " }
        contentTypeCounts.entries.sortedBy { it.value }.forEach { logger.info { "${it.key}: ${it.value}" } }
    }

    private fun increment(value: String, map: MutableMap<String, Int>) {
        map[value] = 1 + (map[value] ?: 0)
    }
}

class EpicDocumentReferenceService(epicClient: EpicClient) : BaseEpicService<DocumentReference>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DocumentReference"
    override val fhirResourceType = DocumentReference::class.java

    fun getDocumentReferences(tenant: Tenant, patientFhirId: String, category: String): List<DocumentReference> {
        val parameters = mapOf("patient" to patientFhirId, "category" to category)
        return getResourceListFromSearch(tenant, parameters)
    }
}
