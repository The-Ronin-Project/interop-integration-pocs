package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.dataloader.epic.Code
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

/*
    This is here to count the total number of LAB and laboratory observations by patient.  Could probably be combined
    with the regular observation loader if we ever need to use this stuff again.
 */
class ObservationLabCounter(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val observationService = EpicDateLimitedObservationService(epicClient)

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        filename: String = "observations.csv"
    ) {
        logger.info { "Loading observations" }
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""MRN","Category","Count"""")
            writer.newLine()

            var totalTime: Long = 0
            patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                val executionTime = measureTimeMillis {
                    val run = runCatching {
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "laboratory",
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "LAB",
                            mrn,
                            writer
                        )
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
        logger.info { "Done loading observations" }
    }

    private fun loadAndWriteObservations(
        patient: Patient,
        tenant: Tenant,
        category: String,
        mrn: String,
        writer: BufferedWriter
    ) {
        val observations = getObservationsForPatient(patient, tenant, category)
        writer.write(""""$mrn","$category",${observations.size}""")
        writer.newLine()
        writer.flush()
        logger.info { "$mrn: ${observations.size} observations for category $category" }
    }

    private fun getObservationsForPatient(
        patient: Patient,
        tenant: Tenant,
        category: String
    ): Map<Code, Observation> =
        observationService.findObservationsByPatient(tenant, listOf(patient.id!!.value!!), listOf(category), null)
            .mapNotNull {
                it.code?.coding?.map { coding ->
                    Pair(
                        Code(
                            coding.system?.value ?: "",
                            coding.code?.value ?: "",
                            coding.display?.value ?: ""
                        ),
                        it
                    )
                }
            }.flatten().associate { it.first to it.second }
}
