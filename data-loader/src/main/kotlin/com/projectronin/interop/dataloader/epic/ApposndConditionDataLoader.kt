package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.AppointmentService
import com.projectronin.interop.dataloader.epic.service.LocationService
import com.projectronin.interop.dataloader.epic.service.PatientService
import com.projectronin.interop.ehr.epic.EpicConditionService
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess

fun main() {
    ApposndConditionDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class ApposndConditionDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1810"
    override val tenantMnemonic = "apposnd"
    private val patientService = PatientService(epicClient)
    private val locationService = LocationService(epicClient)
    private val appointmentService = AppointmentService(epicClient)
    private val conditionService = EpicConditionService(epicClient)

    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        val locations =
            listOf(
                "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03",
                "etKZzJux8VWCn.v-YDz2ZLhdhUwhVwqE082St.Jnq1eDXmuSzU9D4HAOFAP3RHkzY3",
                "en3vmXpNOEYzO2ZFTnH-zcZWjVUInE7.WHpB4gifjyYI3",
                "eJoWPNHZJG0jGSceogBCWb-hb2VKYzZ3B4QhRSKs-vb83",
                "e4aPTZoZLqOja.QwzaEzp0A3",
            )
        val conditionCategories =
            listOf(
                "problem-list-item",
                "encounter-diagnosis",
            )
        logger.info { "Loading conditions" }
        val totalConditions = mutableMapOf<String, List<Condition>>()

        locations.forEach { loc ->
            logger.info { "Loading conditions for $loc" }

            val run =
                runCatching {
                    logger.info { "Loading conditions for $loc" }
                    val identifier = locationService.getDepartmentIdentifierFromFhirID(tenant, loc) ?: return@runCatching
                    logger.info { "Loading appointments for $identifier" }
                    val epicAppointments =
                        appointmentService.getEpicAppointmentsFromDepartmentIdentifier(tenant, identifier, 14, 14)
                    logger.info { "Found ${epicAppointments.size} appointments" }
                    val patientIdentifiers =
                        epicAppointments.map {
                            it.patientIDs.first()
                        }.map {
                            Identifier(value = it.id.asFHIR(), system = Uri(it.type))
                        }.distinct()
                    logger.info { "Finding patients" }
                    val patients = patientService.patientsFromIdentifiers(tenant, patientIdentifiers)
                    logger.info { "Found ${patients.size} patients" }
                    val conditionMap =
                        patients.associate {
                            logger.info { "Finding for patient ${it.id?.value}" }
                            it.id?.value!! to
                                conditionCategories.map { cat ->
                                    conditionService.findConditions(tenant, it.id!!.value!!, cat, "active")
                                }.flatten()
                        }
                    logger.info { "Found ${conditionMap.size} conditions" }
                    totalConditions.putAll(conditionMap)
                }

            if (run.isFailure) {
                val exception = run.exceptionOrNull()
                logger.error(exception) { "Error processing $loc: ${exception?.message}" }
            }
        }
        logger.info { "found ${totalConditions.values.flatten().size} conditions" }

        totalConditions.forEach {
            writeAndUploadConditions(tenant, it.key, it.value, timeStamp)
        }

        logger.info { "Done loading observations" }
    }

    private fun writeAndUploadConditions(
        tenant: Tenant,
        patientFhirID: String,
        conditions: List<Condition>,
        timeStamp: String,
    ) {
        if (conditions.isEmpty()) return
        val fileDirectory = "loaded/conditions"
        val fileName = "$fileDirectory/$patientFhirID.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        logger.info { "Writing $fileName and uploading to OCI" }
        writeFile(fileName, conditions)
        uploadFile(fileName, tenant, "conditions", timeStamp)
    }
}
