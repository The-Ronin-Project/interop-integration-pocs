package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.ObservationViaConditionService
import com.projectronin.interop.ehr.epic.EpicConditionService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.system.measureTimeMillis

@Suppress("ktlint:standard:max-line-length")
class ConditionDataLoader(
    epicClient: EpicClient,
) : BaseEpicDataLoader() {
    private val conditionService = EpicConditionService(epicClient)
    private val observationService = ObservationViaConditionService(epicClient)
    private val resourceType = "conditions"
    override val jira = "Prior to paradigm change"

    override fun main() = TODO("Prior to paradigm change")

    /**
     * Attempts to load conditions through a patient search.  Writes them to a file named "condition_mrn.json" and
     * uploads them to the OCI experimentation bucket under the given timestamp.  If [loadObservations] is true, it will
     * look for references to observations in the staging field and load them, too..  They'll be placed in a file named
     * "observations_from_conditions_mrn.json".
     */
    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        timeStamp: String = System.currentTimeMillis().toString(),
        loadObservations: Boolean = false,
    ) {
        logger.info { "Loading conditions" }

        var totalTime: Long = 0
        patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
            val totalConditions = mutableListOf<Condition>()

            val executionTime =
                measureTimeMillis {
                    val run =
                        runCatching {
                            totalConditions.addAll(
                                loadConditions(patient, tenant, "encounter-diagnosis", mrn),
                            )
                            totalConditions.addAll(
                                loadConditions(patient, tenant, "problem-list-item", mrn),
                            )
                        }

                    if (run.isFailure) {
                        val exception = run.exceptionOrNull()
                        logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
                    }

                    writeAndUploadConditions(tenant, mrn, totalConditions, timeStamp)

                    if (loadObservations) {
                        val totalObservations =
                            totalConditions
                                .filter { condition ->
                                    condition.stage.any { it.assessment.isNotEmpty() }
                                }
                                .map { condition ->
                                    loadObservations(tenant, condition.id!!.value!!)
                                }.flatten()
                        writeAndUploadObservations(tenant, mrn, totalObservations, timeStamp)
                    }
                }

            totalTime += executionTime
            logger.info { "Completed ${index + 1} of ${patientsByMrn.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
        }

        logger.info { "Done loading conditions" }
    }

    private fun loadConditions(
        patient: Patient,
        tenant: Tenant,
        category: String,
        mrn: String,
    ): List<Condition> {
        logger.info { "Searching for $category for mrn $mrn" }
        val conditions = conditionService.findConditions(tenant, patient.id!!.value!!, category, "active")
        logger.info { "Found ${conditions.size} conditions for category $category" }
        return conditions
    }

    private fun loadObservations(
        tenant: Tenant,
        conditionId: String,
    ): List<Observation> {
        logger.info { "Searching for observations referenced in condition $conditionId" }
        val observations = observationService.findObservationsByCondition(tenant, conditionId)
        logger.info { "${observations.size} observations found for $conditionId" }
        return observations
    }

    private fun writeAndUploadConditions(
        tenant: Tenant,
        mrn: String,
        conditions: List<Condition>,
        timeStamp: String,
    ) {
        if (conditions.isEmpty()) return

        val fileDirectory = "loaded/$resourceType"
        val fileName = "$fileDirectory/$mrn.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        logger.info { "Writing $fileName and uploading to OCI" }
        writeFile(fileName, conditions)
        uploadFile(fileName, tenant, resourceType, timeStamp)
    }

    private fun writeAndUploadObservations(
        tenant: Tenant,
        mrn: String,
        observations: List<Observation>,
        timeStamp: String,
    ) {
        if (observations.isEmpty()) return

        val fileDirectory = "loaded/observations_from_conditions_stage"
        val fileName = "$fileDirectory/$mrn.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        logger.info { "Writing $fileName and uploading to OCI" }
        writeFile(fileName, observations)
        uploadFile(fileName, tenant, "observations_from_conditions_stage", timeStamp)
    }
}
