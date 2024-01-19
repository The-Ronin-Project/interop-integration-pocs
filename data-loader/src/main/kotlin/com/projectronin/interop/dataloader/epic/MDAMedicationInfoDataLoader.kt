package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.ehr.epic.EpicMedicationRequestService
import com.projectronin.interop.ehr.epic.EpicMedicationService
import com.projectronin.interop.ehr.epic.EpicMedicationStatementService
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import java.time.LocalDate
import kotlin.system.exitProcess

fun main() {
    MDAMedicationInfoDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class MDAMedicationInfoDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1791"
    override val tenantMnemonic = "mdaoc"
    private val medicationRequestService = EpicMedicationRequestService(epicClient)
    private val medicationStatementService = EpicMedicationStatementService(epicClient)
    private val medicationService = EpicMedicationService(epicClient, batchSize = 5)

    override fun main() {
        val patientsByMrn = getPatientsForMRNs()
        val timeStamp = System.currentTimeMillis().toString()
        val today = LocalDate.now()
        val startDate = today.minusDays(30)
        val endDate = today
        logger.info { "Loading Encounters" }
        val totalMedications = mutableListOf<Medication>()
        val totalMedicationIds = mutableListOf<String>()
        val totalMedStatements = mutableMapOf<String, List<MedicationStatement>>()
        val totalMedRequests = mutableMapOf<String, List<MedicationRequest>>()
        patientsByMrn.forEach { entry ->
            val patient = entry.value
            val mrn = entry.key
            val fhirId = patient.id?.value!!
            logger.info { "Loading Medications for $mrn" }
            val run =
                runCatching {
                    // discover medication references from patients
                    val requests =
                        medicationRequestService.getMedicationRequestByPatient(
                            tenant,
                            fhirId,
                        )
                    totalMedRequests[fhirId] = requests
                    logger.info { "Found ${requests.size} requests" }
                    val statements =
                        medicationStatementService.getMedicationStatementsByPatientFHIRId(
                            tenant,
                            fhirId,
                        )
                    totalMedStatements[fhirId] = statements
                    logger.info { "Found ${statements.size} statements" }
                    val medicationIdFromStatements =
                        statements.mapNotNull {
                            val medication = it.medication!!
                            if (medication.type == DynamicValueType.REFERENCE) {
                                (medication.value as Reference).decomposedId()
                            } else {
                                logger.info { "MedicationStatement had medication of type ${medication.type}" }
                                null
                            }
                        }
                    val medicationIdFromRequests =
                        requests.mapNotNull {
                            val medication = it.medication!!
                            if (medication.type == DynamicValueType.REFERENCE) {
                                (medication.value as Reference).decomposedId()
                            } else {
                                logger.info { "MedicationRequest had medication of type ${medication.type}" }
                                null
                            }
                        }
                    val allMedicationIds = (medicationIdFromStatements + medicationIdFromRequests).distinct()
                    logger.info { "Found ${allMedicationIds.size} Medication IDs" }
                    val newMedicationsIds = allMedicationIds - totalMedicationIds
                    logger.info { "Of which ${newMedicationsIds.size} are new" }
                    logger.info { "Searching for Medications" }
                    val medications =
                        medicationService.getMedicationsByFhirId(
                            tenant = tenant,
                            newMedicationsIds,
                        )
                    totalMedicationIds.addAll(newMedicationsIds)
                    logger.info { "Found ${medications.size} Medications" }
                    totalMedications.addAll(medications)
                }

            if (run.isFailure) {
                val exception = run.exceptionOrNull()
                logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
            }
        }
        logger.info { "Starting to resolve references" }
        logger.info { "There are currently ${totalMedicationIds.size} medication ids" }
        // resolve all the new medications we've gotten
        var count = 0
        do {
            count += 1
            logger.info { "Starting to resolve references on loop $count" }
            val newIngredientIds = getNewReferences(totalMedications, totalMedicationIds)
            logger.info { "Found ${newIngredientIds.size} new ingredients" }
            val newMedications =
                medicationService.getMedicationsByFhirId(
                    tenant = tenant,
                    newIngredientIds,
                )
            totalMedicationIds.addAll(newIngredientIds)
            totalMedications.addAll(newMedications)
            logger.info { "There are currently ${totalMedicationIds.size} medications" }
        } while (newIngredientIds.isNotEmpty() && count < 4) // since this is a while loop, this is just a safety valve

        logger.info { "Found ${totalMedications.size} medications" }

        totalMedications.forEach {
            writeAndUploadResources(tenant, it.id?.value!!, listOf(it), timeStamp, dryRun = false)
        }

        logger.info { "Found ${totalMedRequests.map { it.value }.flatten().size} medication requests" }

        totalMedRequests.forEach {
            writeAndUploadResources(tenant, it.key, it.value, timeStamp, dryRun = false)
        }

        logger.info { "Found ${totalMedStatements.map { it.value }.flatten().size} medication statements" }

        totalMedStatements.forEach {
            writeAndUploadResources(tenant, it.key, it.value, timeStamp, dryRun = false)
        }
        logger.info { "Done loading Medications" }
    }

    private fun getNewReferences(
        medications: List<Medication>,
        foundIds: List<String>,
    ): List<String> {
        val allIngredients =
            medications.map {
                val ingredients = it.ingredient
                ingredients.mapNotNull {
                    if (it.item?.type == DynamicValueType.REFERENCE) {
                        val reference = it.item?.value as Reference
                        if (reference.decomposedType() == "Medication") {
                            reference.decomposedId()!!
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }.flatten()
        logger.info { "There are ${allIngredients.size} ingredient references" }
        val newIngredients =
            allIngredients.filter {
                !foundIds.contains(it)
            }
        logger.info { "Of which ${newIngredients.size} are new" }
        return newIngredients
    }
}
