package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.ObservationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.io.path.createDirectory
import kotlin.system.measureTimeMillis

class ObservationDataLoader(
    epicClient: EpicClient
) : BaseEpicDataLoader() {
    private val observationService = ObservationService(epicClient)
    private val resourceType = "observations"
    override val jira = "Prior to paradigm change"
    override fun main() = TODO("Prior to paradigm change")

    /**
     * Attempts to load observations through a patient search.  Writes them to a file named "observation_mrn.json" and
     * uploads them to the OCI experimentation bucket under the given timestamp.  While this will find most observations,
     * there a few that won't be aren't available via patient search.  To get those, load the patient's conditions and
     * use the references to retrieve specific observations.  Some will be duplicates, but some, like SDEs won't.
     */
    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        startDate: LocalDate? = null,
        interestedObservations: List<String> = emptyList(),
        timeStamp: String = System.currentTimeMillis().toString()
    ) {
        logger.info { "Loading observations" }

        var totalTime: Long = 0
        patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
            val totalObservations = mutableListOf<Observation>()

            val executionTime = measureTimeMillis {
                val run = runCatching {
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "genomics",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "laboratory",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "social-history",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "functional-mental-status",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "http://snomed.info/sct|384821006",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "http://snomed.info/sct|118228005",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "smartdata",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                    totalObservations.addAll(
                        loadObservations(
                            patient,
                            tenant,
                            "vital-signs",
                            interestedObservations,
                            startDate,
                            mrn
                        )
                    )
                }

                if (run.isFailure) {
                    val exception = run.exceptionOrNull()
                    logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
                }

                writeAndUploadObservations(tenant, mrn, totalObservations, timeStamp)
            }

            totalTime += executionTime
            logger.info { "Completed ${index + 1} of ${patientsByMrn.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
        }
        logger.info { "Done loading observations" }
    }

    private fun loadObservations(
        patient: Patient,
        tenant: Tenant,
        category: String,
        interestedCategories: List<String>,
        startDate: LocalDate?,
        mrn: String
    ): List<Observation> {
        if (interestedCategories.isNotEmpty() && !interestedCategories.contains(category)) return listOf()

        logger.info { "Searching for $category for mrn $mrn" }
        val observations = observationService.findObservationsByPatient(
            tenant,
            listOf(patient.id!!.value!!),
            listOf(category),
            startDate
        )
        logger.info { "Found ${observations.size} observations for category $category" }
        return observations
    }

    private fun writeAndUploadObservations(tenant: Tenant, mrn: String, observations: List<Observation>, timeStamp: String) {
        if (observations.isEmpty()) return

        val fileDirectory = "loaded/$resourceType"
        val fileName = "$fileDirectory/$mrn.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        logger.info { "Writing $fileName and uploading to OCI" }
        writeFile(fileName, observations)
        uploadFile(fileName, tenant, resourceType, timeStamp)
    }
}
