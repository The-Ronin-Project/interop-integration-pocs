package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.ehr.epic.EpicEncounterService
import com.projectronin.interop.fhir.r4.resource.Encounter
import java.time.LocalDate
import kotlin.system.exitProcess

fun main() {
    PSJEncounterDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class PSJEncounterDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1770"
    override val tenantMnemonic = "v7r1eczk"
    private val encounterService = EpicEncounterService(epicClient)
    override fun main() {
        val patientsByMrn = getPatientsForMRNs(getMRNs())
        val timeStamp = System.currentTimeMillis().toString()
        val today = LocalDate.now()
        val startDate = today.minusDays(60)
        val endDate = today
        logger.info { "Loading Encounters" }
        val totalEncounters = mutableMapOf<String, List<Encounter>>()
        patientsByMrn.forEach {
            val patient = it.value
            val mrn = it.key
            val fhirId = patient.id?.value!!
            logger.info { "Loading encounters for $mrn" }
            val run = runCatching {
                val encounters = encounterService.findPatientEncounters(
                    tenant = tenant,
                    patientFhirId = fhirId,
                    startDate = startDate,
                    endDate = endDate
                )
                logger.info { "Found ${encounters.size} encounters" }
                totalEncounters.put(mrn, encounters)
            }

            if (run.isFailure) {
                val exception = run.exceptionOrNull()
                logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
            }
        }
        logger.info { "Found ${totalEncounters.values.flatten().size} total encounters" }

        totalEncounters.forEach {
            writeAndUploadResources<Encounter>(tenant, it.key, it.value, timeStamp, dryRun = false)
        }

        logger.info { "Done loading encounters" }
    }
}
