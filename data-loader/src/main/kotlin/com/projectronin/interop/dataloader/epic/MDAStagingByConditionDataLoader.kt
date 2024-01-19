package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.ConditionBundleService
import com.projectronin.interop.dataloader.epic.service.ObservationBundleService
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.Observation
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main() {
    MDAStagingByConditionDataLoader().main()
    // INT-1791 has a note: This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

@Suppress("ktlint:standard:max-line-length")
class MDAStagingByConditionDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1960"
    override val tenantMnemonic = "mdaoc"
    private val conditionService = ConditionBundleService(epicClient)
    private val observationService = ObservationBundleService(epicClient)

    override fun main() {
        val patientsByMrn = getPatientsForMRNs()
        val patientCount = patientsByMrn.size
        logger.info { "$patientCount patients found" }
        var totalTime: Long = 0
        var totalTimeStaging: Long = 0
        var totalTimeNoStaging: Long = 0
        var totalPatientCount: Long = 0
        var totalPatientHadStaging: Long = 0
        var totalConditionCount = 0
        var totalObservationCount = 0
        val timeStamp = System.currentTimeMillis().toString()
        patientsByMrn.forEach { entry ->
            val patient = entry.value
            val mrn = entry.key
            val fhirId = patient.id?.value!!
            logger.info { "Loading staging Observations via Conditions for mrn $mrn" }
            var hasStaging = false
            val executionTime =
                measureTimeMillis {
                    val run =
                        runCatching {
                            val bundle =
                                conditionService.findConditionsWithStaging(
                                    tenant,
                                    fhirId,
                                )
                            val conditionCount =
                                bundle.entry
                                    .mapNotNull { it.resource }
                                    .filterIsInstance(Condition::class.java).count()

                            val observationBundles = mutableListOf<Bundle>()
                            bundle.entry
                                .mapNotNull { it.resource }
                                .filterIsInstance(Condition::class.java)
                                .forEach { condition ->
                                    // logger.info { "Getting staging Observations from Condition ${condition.id!!.value}" }
                                    condition.stage.flatMap { stage ->
                                        stage.assessment.filter { ref ->
                                            ref.decomposedType() == "Observation"
                                        }
                                    }.forEach { reference ->
                                        observationBundles.add(
                                            observationService.getObservationBundle(
                                                tenant,
                                                reference.decomposedId()!!,
                                            ),
                                        )
                                    }
                                }
                            val observationCount = observationBundles.flatMap { it.entry }.size
                            logger.info { "$observationCount staging Observations in $conditionCount Conditions for mrn $mrn" }
                            if (observationCount > 0) {
                                hasStaging = true
                                totalPatientHadStaging += 1
                            }
                            observationBundles.forEach { bun ->
                                writeAndUploadResources(
                                    tenant,
                                    mrn,
                                    bun.entry.map { it.resource as Observation },
                                    timeStamp,
                                    dryRun = false,
                                )
                            }
                            totalConditionCount += conditionCount
                            totalObservationCount += observationCount
                        }

                    if (run.isFailure) {
                        val exception = run.exceptionOrNull()
                        logger.error(exception) { "Error processing mrn $mrn: ${exception?.message}" }
                    } else {
                        totalPatientCount += 1
                    }
                }
            logger.info { "Execution time: $executionTime ms for mrn $mrn" }
            totalTime += executionTime
            if (hasStaging) {
                totalTimeStaging += executionTime
            } else {
                totalTimeNoStaging += executionTime
            }
        }
        val totalPatientNoStaging = patientCount - totalPatientHadStaging
        logger.info { "Found $totalObservationCount staging Observations in $totalConditionCount Conditions for $patientCount patients. " }
        logger.info { "Execution time: $totalTime ms, with $totalPatientCount successful runs and ${patientCount - totalPatientCount} errors" }
        logger.info {
            "Execution time average per patient: ${
                if (patientCount > 0) {
                    totalTime / patientCount
                } else {
                    "N/A"
                }
            } ms"
        }
        logger.info {
            "Execution time average per patient with staging Observations: ${
                if (totalPatientHadStaging > 0) {
                    totalTimeStaging / totalPatientHadStaging
                } else {
                    "N/A"
                }
            } ms"
        }
        logger.info {
            "Execution time average per patient without staging Observations: ${
                if (totalPatientNoStaging > 0) {
                    totalTimeNoStaging / totalPatientNoStaging
                } else {
                    "N/A"
                }
            } ms"
        }
        logger.info {
            "$totalPatientHadStaging of $patientCount patients had staging Observations (${
                if (patientCount > 0) {
                    ((totalPatientHadStaging * 100) / patientCount)
                } else {
                    "N/A"
                }
            }%)."
        }
        logger.info { "Done loading staging Observations" }
    }
}
