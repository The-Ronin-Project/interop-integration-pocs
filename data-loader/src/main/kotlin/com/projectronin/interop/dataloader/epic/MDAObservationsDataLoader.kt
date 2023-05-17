package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.ObservationService
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess

fun main() {
    MDAObservationsDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class MDAObservationsDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1786"
    override val tenantMnemonic = "mdaoc"
    private val observationService = ObservationService(epicClient)
    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        val startDate = LocalDate.now().minusMonths(1)
        val observationCategories = listOf(
            "genomics",
            "laboratory",
            "social-history",
            "functional-mental-status",
            "http://snomed.info/sct|384821006",
            "http://snomed.info/sct|118228005",
            "smartdata",
            "vital-signs"
        )
        logger.info { "Loading observations" }

        val totalObservations = mutableMapOf<String, List<Observation>>()

        val run = runCatching {
            val patients = this.javaClass.getResourceAsStream("/mrns.txt")!!.bufferedReader().readLines()
            logger.info { "Found ${patients.size} patients" }
            val observationMap = patients.associateWith {
                if (it == "") return@associateWith emptyList()
                observationCategories.map { obv ->
                    try {
                        observationService.findObservationsByPatient(
                            tenant,
                            listOf(it),
                            listOf(obv),
                            startDate
                        )
                    } catch (e: Exception) {
                        logger.error {
                            "Patient $it failed: ${e.message} "
                        }
                        emptyList()
                    }
                }.flatten()
            }
            logger.info { "Found ${observationMap.size} observations" }
            totalObservations.putAll(observationMap)
        }

        if (run.isFailure) {
            val exception = run.exceptionOrNull()
            logger.error(exception) { "Error processing: ${exception?.message}" }
        }
        totalObservations.forEach {
            writeAndUploadObservations(tenant, it.key, it.value, timeStamp)
        }

        logger.info { "Done loading observations" }
    }

    private fun writeAndUploadObservations(
        tenant: Tenant,
        patientFhirID: String,
        observations: List<Observation>,
        timeStamp: String
    ) {
        if (observations.isEmpty()) return
        val fileDirectory = "loaded/observations"
        val fileName = "$fileDirectory/$patientFhirID.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        logger.info { "Writing $fileName and uploading to OCI" }
        writeFile(fileName, observations)
        uploadFile(fileName, tenant, "observations", timeStamp)
    }
}
