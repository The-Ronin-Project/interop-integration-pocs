package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.epic.EpicMedicationService
import com.projectronin.interop.ehr.epic.EpicMedicationStatementService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class MedicationDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val medicationService = EpicMedicationService(epicClient, 5)
    private val medicationStatementService = EpicMedicationStatementService(epicClient)
    private val medicationRequestService = EpicMedicationRequestService(epicClient)

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, filename: String = "medications.csv") {
        val allPatients = patientsByMrn.values
        val allMedicationIds = mutableSetOf<String>()
        // allMedicationIds += loadMedicationIDsFromAdministrations(tenant, allPatients)
        // allMedicationIds += loadMedicationIDsFromDispenses(tenant, allPatients)
        allMedicationIds += loadMedicationIDsFromRequests(tenant, allPatients)
        allMedicationIds += loadMedicationIDsFromStatements(tenant, allPatients)

        logger.info { "Loading ${allMedicationIds.size} Medications" }
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""Medication FHIR ID","Escaped JSON"""")
            writer.newLine()
            loadAndWriteMedications(tenant, allMedicationIds, writer)
        }
        logger.info { "Done loading Medications" }
    }

    private fun loadMedicationIDsFromAdministrations(tenant: Tenant, patients: Collection<Patient>): Set<String> {
        TODO("Only available in Netherlands")
    }

    private fun loadMedicationIDsFromDispenses(tenant: Tenant, patients: Collection<Patient>): Set<String> {
        TODO("Only available in Netherlands")
    }

    private fun loadMedicationIDsFromRequests(tenant: Tenant, patients: Collection<Patient>): Set<String> {
        logger.info { "Loading medication requests" }

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
        logger.info { "Done loading medication statements. Found ${medicationRequests.size}" }

        return medicationRequests.mapNotNull {
            val medication = it.medication!!
            if (medication.type == DynamicValueType.REFERENCE) {
                (medication.value as Reference).decomposedId()
            } else {
                logger.info { "MedicationStatement had medication of type ${medication.type}" }
                null
            }
        }.toSet()
    }

    private fun loadMedicationIDsFromStatements(tenant: Tenant, patients: Collection<Patient>): Set<String> {
        logger.info { "Loading medication statements" }

        var totalTime: Long = 0
        val medicationStatements = patients.flatMapIndexed { index, patient ->
            val statements: List<MedicationStatement>
            val executionTime = measureTimeMillis {
                statements = medicationStatementService.getMedicationStatementsByPatientFHIRId(
                    tenant,
                    patient.id!!.value!!
                )
            }

            totalTime += executionTime
            logger.info { "Completed ${index + 1} of ${patients.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }

            statements
        }
        logger.info { "Done loading medication statements. Found ${medicationStatements.size}" }

        return medicationStatements.mapNotNull {
            val medication = it.medication!!
            if (medication.type == DynamicValueType.REFERENCE) {
                (medication.value as Reference).decomposedId()
            } else {
                logger.info { "MedicationStatement had medication of type ${medication.type}" }
                null
            }
        }.toSet()
    }

    private fun loadAndWriteMedications(
        tenant: Tenant,
        medicationIds: Set<String>,
        writer: BufferedWriter
    ) {
        var totalTime: Long = 0

        val chunks = medicationIds.chunked(25)
        chunks.forEachIndexed { index, ids ->
            val executionTime = measureTimeMillis {
                getMedications(tenant, ids).values.forEach { medication ->
                    writeMedication(medication, writer)
                }
                writer.flush()
            }

            totalTime += executionTime
            logger.info { "Completed batch ${index + 1} of ${chunks.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
        }
    }

    private fun getMedications(tenant: Tenant, medicationIds: List<String>): Map<String, Medication> {
        val run = runCatching {
            medicationService.getMedicationsByFhirId(tenant, medicationIds).associateBy { it.id!!.value!! }
        }

        if (run.isFailure) {
            val exception = run.exceptionOrNull()
            logger.error(exception) { "Error processing medications: ${exception?.message}" }
        }

        return run.getOrNull()!!
    }

    private fun writeMedication(
        medication: Medication,
        writer: BufferedWriter
    ) {
        val json = JacksonManager.objectMapper.writeValueAsString(medication)
        val escapedJson = StringEscapeUtils.escapeCsv(json)
        writer.write(""""${medication.id!!.value}",$escapedJson""")
        writer.newLine()
    }
}

class EpicMedicationRequestService(epicClient: EpicClient) :
    BaseEpicService<MedicationRequest>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/MedicationRequest"
    override val fhirResourceType = MedicationRequest::class.java

    fun getMedicationRequestsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String
    ): List<MedicationRequest> {
        val parameters = mapOf("patient" to patientFHIRId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
