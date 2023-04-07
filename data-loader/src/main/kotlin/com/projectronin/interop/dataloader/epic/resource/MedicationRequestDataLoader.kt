package com.projectronin.interop.dataloader.epic.resource
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis
class MedicationRequestDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val medicationRequestService = EpicMedicationRequestService(epicClient)

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, filename: String = "medicationRequests.csv") {
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""Medication Request FHIR ID",,"Escaped JSON"""")
            writer.newLine()

            loadAndWriteMedicationRequests(
                tenant,
                patientsByMrn.values,
                writer
            )
        }

        logger.info { "Done loading Medication Requests" }
    }

    private fun loadAndWriteMedicationRequests(tenant: Tenant, patients: Collection<Patient>, writer: BufferedWriter) {
        var totalTime: Long = 0
        val medicationRequests = patients.flatMapIndexed { index, patient ->
            val requests: List<MedicationRequest>
            val executionTime = measureTimeMillis {
                requests = medicationRequestService.getMedicationRequestsByPatientFHIRId(
                    tenant,
                    patient.id!!.value!!
                )
            }

            totalTime += executionTime
            logger.info { "Completed ${index + 1} of ${patients.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }

            requests
        }
        logger.info { "Done loading medication requests. Found ${medicationRequests.size}" }
        val chunks = medicationRequests.chunked(25)
        chunks.forEach { medicationRequestChunk ->
            medicationRequestChunk.forEach {
                writeMedicationRequest(it, writer)
            }
            writer.flush()
        }
    }

    private fun writeMedicationRequest(
        medicationRequest: MedicationRequest,
        writer: BufferedWriter
    ) {
        val json = JacksonManager.objectMapper.writeValueAsString(medicationRequest)
        val escapedJson = StringEscapeUtils.escapeCsv(json)
        writer.write(""""${medicationRequest.id!!.value}",$escapedJson""")
        writer.newLine()
    }
}
