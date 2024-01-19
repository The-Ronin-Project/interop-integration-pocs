package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.service.BinaryService
import com.projectronin.interop.dataloader.epic.service.DocumentReferenceService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
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
    httpClient: HttpClient,
) : BaseEpicDataLoader() {
    private val documentReferenceService = DocumentReferenceService(epicClient)
    private val binaryService = BinaryService(epicClient, authenticationBroker, httpClient)
    override val jira = "Prior to paradigm change"

    override fun main() = TODO("Prior to paradigm change")

    private val clinicalNoteCategory = "clinical-note"
    private val radiologyResultCategory = "imaging-result"

    private var totalAttachments = 0
    private var totalDatas = 0
    private var totalUrls = 0
    private var totalBinaries = 0
    private val contentTypeCounts = mutableMapOf<String, Int>()
    private var statusCounts = mutableMapOf<String, Int>()
    private var docStatusCounts = mutableMapOf<String, Int>()

    /**
     * Loads document references via patient search, then attempts to download the attached documents.  Writes the
     * document references to a file named "document_reference_mrn.json" and the individual attachments to files named
     * "fhirId-binary#.extension".  FhirId is the id of the document reference, # is an index to make sure multiple
     * attachments under the same document reference have different names, and extension is the file's extension.
     */
    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        timeStamp: String = System.currentTimeMillis().toString(),
    ) {
        runCatching { Paths.get("loaded/docRef/binary").createDirectories() }

        patientsByMrn.map { (mrn, patient) ->
            logger.info { "Loading DocumentReferences for MRN: $mrn" }
            val documentReferences = documentReferenceService.getDocumentReferences(tenant, patient.id!!.value!!)

            val docRefFileName = "loaded/docRef/document_reference_$mrn.json"
            BufferedWriter(FileWriter(File(docRefFileName))).use { writer ->
                writer.write(JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentReferences))
            }
            expOCIClient.uploadExport(tenant, "document_reference", docRefFileName, timeStamp)

            logger.info { "Writing ${documentReferences.size} document references to files." }
            documentReferences.forEachIndexed { docIndex, docRef ->
                increment(docRef.status!!.value!!, statusCounts)
                docRef.docStatus?.value?.let { increment(it, docStatusCounts) }

                docRef.content.forEachIndexed { contentIndex, content ->
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
                                "text/rtf" -> "rtf"
                                "image/jpeg" -> "jpg"
                                "application/octet-stream" -> null // Not totally sure what to do with this one
                                else -> null
                            }?.let { extension ->
                                val binaryFileName = "loaded/docRef/binary/${docRef.id!!.value!!}-binary$contentIndex.$extension"
                                val file = File(binaryFileName)
                                val binaryFhirId = it.removePrefix("Binary/")

                                if (listOf("pdf", "jpg").contains(extension)) {
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

                                if (file.exists()) {
                                    expOCIClient.uploadExport(tenant, "binary", binaryFileName, timeStamp)
                                }
                            } ?: logger.warn { "Attachment content type we're not prepared to handle: ${attachment.contentType?.value}" }
                        }
                    }
                    attachment.contentType?.value?.let { increment(it, contentTypeCounts) }
                }
                logger.info { "Retrieved doc ${docIndex + 1} of ${documentReferences.size}" }
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

    private fun increment(
        value: String,
        map: MutableMap<String, Int>,
    ) {
        map[value] = 1 + (map[value] ?: 0)
    }
}
