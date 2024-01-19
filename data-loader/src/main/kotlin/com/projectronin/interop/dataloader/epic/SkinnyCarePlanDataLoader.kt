package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.service.CarePlanCategory
import com.projectronin.interop.dataloader.epic.service.CarePlanService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Attempts to load all of the patients care plans without following references and pulling in documents.
 * Writes them to a file named "mrn-careplans-category.json"
 */
class SkinnyCarePlanDataLoader(epicClient: EpicClient) : BaseEpicDataLoader() {
    private val epicCarePlanService = CarePlanService(epicClient)
    override val jira = "Prior to paradigm change"

    override fun main() = TODO("Prior to paradigm change")

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        category: CarePlanCategory,
    ) {
        logger.info { "Loading care plans for ${category.category}" }

        patientsByMrn.map { (mrn, patient) ->
            val carePlans = epicCarePlanService.getCarePlansByPatient(tenant, patient.id!!.value!!, category.code)

            if (carePlans.isNotEmpty()) {
                BufferedWriter(FileWriter(File("loaded/$mrn-careplans-${category.category}.json"))).use { writer ->
                    writer.write(
                        JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(carePlans),
                    )
                }
            }
        }

        logger.info { "Done loading care plans" }
    }
}
