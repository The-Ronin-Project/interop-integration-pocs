package com.projectronin.interop.dataloader.cerner

import com.projectronin.interop.dataloader.cerner.service.AppointmentService
import com.projectronin.interop.dataloader.cerner.service.ObservationService
import com.projectronin.interop.ehr.cerner.isPatient
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess

fun main() {
    CernerSandboxObservationsDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}
class CernerSandboxObservationsDataLoader : BaseCernerDataLoader() {
    override val jira = "INT-1666"
    override val tenantMnemonic = "cerncode"
    private val appointmentService = AppointmentService(cernerClient)
    private val observationService = ObservationService(cernerClient)
    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        logger.info { "Loading observations" }
        // val locations = locationService.getSomeLocations(tenant)
        // println(locations)
        val locations = listOf(
            "21304876",
            "21251399",
            "33834827",
            "32216061"
        )
        locations.forEach { loc ->
            val totalObservations = mutableMapOf<String, List<Observation>>()

            val run = runCatching {
                logger.info { "finding appointments" }
                val appointments = appointmentService.getAppointmentFromLocation(tenant, loc)
                logger.info { "Found ${appointments.size} appointments" }

                val patientIds = appointments
                    .map {
                        it.participant
                            .filter { it.isPatient() }
                    }
                    .flatten()
                    .map { it.actor?.decomposedId()!! }
                    .distinct()
                    .take(20)
                logger.info { "Found ${patientIds.size} patients" }
                logger.info { "finding observations" }
                val startDate = LocalDate.now().minusMonths(1)
                val observationMap = patientIds.associateWith {
                    observationService.findObservationsByPatient(
                        tenant,
                        listOf(it),
                        startDate
                    )
                }
                totalObservations.putAll(observationMap)
            }

            if (run.isFailure) {
                val exception = run.exceptionOrNull()
                logger.error(exception) { "Error processing $loc: ${exception?.message}" }
            }
            totalObservations.forEach {
                writeAndUploadObservations(tenant, it.key, it.value, timeStamp)
            }
        }

        logger.info { "Done loading observations" }
    }

    private fun writeAndUploadObservations(tenant: Tenant, patientFhirID: String, observations: List<Observation>, timeStamp: String) {
        if (observations.isEmpty()) return

        val fileDirectory = "loaded/observations"
        val fileName = "$fileDirectory/$patientFhirID.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        logger.info { "Writing $fileName and uploading to OCI" }
        writeFile(fileName, observations)
        uploadFile(fileName, tenant, "observations", timeStamp)
    }
}
