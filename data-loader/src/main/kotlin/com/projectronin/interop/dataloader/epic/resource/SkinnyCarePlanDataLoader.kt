package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Attempts to load all of the patients care plans without following references and pulling in documents.
 * Writes them to a file named "mrn-careplans-category.json"
 */
class SkinnyCarePlanDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val epicCarePlanService = EpicCarePlanService(epicClient)

    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant, category: CarePlanCategory) {
        logger.info { "Loading care plans for ${category.category}" }

        patientsByMrn.map { (mrn, patient) ->
            val carePlans = epicCarePlanService.getCarePlansByPatient(tenant, patient.id!!.value!!, category.code)

            if (carePlans.isNotEmpty()) {
                BufferedWriter(FileWriter(File("loaded/$mrn-careplans-${category.category}.json"))).use { writer ->
                    writer.write(
                        JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(carePlans)
                    )
                }
            }
        }

        logger.info { "Done loading care plans" }
    }
}

class EpicCarePlanService(epicClient: EpicClient) : BaseEpicService<CarePlan>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/CarePlan"
    override val fhirResourceType = CarePlan::class.java

    fun getCarePlansByPatient(tenant: Tenant, patientFhirId: String, category: String): List<CarePlan> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "category" to category
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}

enum class CarePlanCategory(val category: String, val code: String) {
    ONCOLOGY("Oncology", "736378000"),
    LONGITUDINAL("Longitudinal", "38717003"),
    ENCOUNTER_LEVEL("Encounter_Level", "734163000"),
    OUTPATIENT("Outpatient", "736271009"),
    PATIENT_EDUCATION("Patient_Education", "409073007")
}
