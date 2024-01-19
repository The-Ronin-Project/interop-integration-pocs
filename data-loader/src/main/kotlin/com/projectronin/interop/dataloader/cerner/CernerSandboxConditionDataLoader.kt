package com.projectronin.interop.dataloader.cerner

import com.projectronin.interop.dataloader.cerner.service.AppointmentService
import com.projectronin.interop.ehr.cerner.CernerConditionService
import com.projectronin.interop.ehr.cerner.isPatient
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.tenant.config.model.Tenant
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess

fun main() {
    CernerSandboxConditionDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class CernerSandboxConditionDataLoader : BaseCernerDataLoader() {
    override val jira = "INT-1810"
    override val tenantMnemonic = "cerncode"
    private val appointmentService = AppointmentService(cernerClient)
    private val conditionService = CernerConditionService(cernerClient)

    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        logger.info { "Loading conditions" }
        // val locations = locationService.getSomeLocations(tenant)
        // println(locations)
        val locations =
            listOf(
                "21304876",
                "21251399",
                "33834827",
                "32216061",
            )
        val conditionCategories =
            listOf(
                "problem-list-item",
                "encounter-diagnosis",
            )
        val totalConditions = mutableMapOf<String, List<Condition>>()

        locations.forEach { loc ->

            val run =
                runCatching {
                    logger.info { "finding appointments" }
                    val appointments = appointmentService.getAppointmentFromLocation(tenant, loc, 4, 4)
                    logger.info { "Found ${appointments.size} appointments" }

                    val patientIds =
                        appointments
                            .map {
                                it.participant
                                    .filter { it.isPatient() }
                            }
                            .flatten()
                            .map { it.actor?.decomposedId()!! }
                            .distinct()
                            .take(20)
                    logger.info { "Found ${patientIds.size} patients" }
                    logger.info { "finding conditions" }
                    val conditionMap =
                        patientIds.associateWith {
                            logger.info { "finding for patient $it" }
                            conditionCategories.map { cat ->
                                conditionService.findConditions(tenant, it, cat, "active")
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

        logger.info { "Done loading conditions" }
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
