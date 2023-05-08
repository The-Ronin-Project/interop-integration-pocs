package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.AppointmentService
import com.projectronin.interop.dataloader.epic.service.LocationService
import com.projectronin.interop.dataloader.epic.service.ObservationService
import com.projectronin.interop.dataloader.epic.service.PatientService
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess

fun main() {
    ApposndObservationsDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}
class ApposndObservationsDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1666"
    override val tenantMnemonic = "apposnd"
    private val patientService = PatientService(epicClient)
    private val locationService = LocationService(epicClient)
    private val appointmentService = AppointmentService(epicClient)
    private val observationService = ObservationService(epicClient)
    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        val locations = listOf(
            "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03",
            "etKZzJux8VWCn.v-YDz2ZLhdhUwhVwqE082St.Jnq1eDXmuSzU9D4HAOFAP3RHkzY3",
            "en3vmXpNOEYzO2ZFTnH-zcZWjVUInE7.WHpB4gifjyYI3",
            "eJoWPNHZJG0jGSceogBCWb-hb2VKYzZ3B4QhRSKs-vb83",
            "e4aPTZoZLqOja.QwzaEzp0A3"
        )
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
        locations.forEach { loc ->
            logger.info { "Loading observations for $loc" }

            val totalObservations = mutableMapOf<String, List<Observation>>()

            val run = runCatching {
                val identifier = locationService.getDepartmentIdentifierFromFhirID(tenant, loc) ?: return
                logger.info { "Loading appointments for $identifier" }
                val epicAppointments = appointmentService.getEpicAppointmentsFromDepartmentIdentifier(tenant, identifier)
                logger.info { "Found ${epicAppointments.size} appointments" }
                val patientIdentifiers = epicAppointments.map {
                    it.patientIDs.first()
                }.map {
                    Identifier(value = it.id.asFHIR(), system = Uri(it.type))
                }.distinct()
                logger.info { "Finding patients" }
                val patients = patientService.patientsFromIdentifiers(tenant, patientIdentifiers)
                logger.info { "Found ${patients.size} patients" }
                val observationMap = patients.associate {
                    logger.info { "Finding for patient ${it.id?.value}" }
                    it.id?.value!! to observationCategories.map { obv ->
                        observationService.findObservationsByPatient(
                            tenant,
                            listOf(it.id?.value!!),
                            listOf(obv),
                            null
                        )
                    }.flatten()
                }
                logger.info { "Found ${observationMap.size} observations" }
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
