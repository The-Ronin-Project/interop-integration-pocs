package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.tenant.config.model.Tenant

class CarePlanService(epicClient: EpicClient) : BaseEpicService<CarePlan>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/CarePlan"
    override val fhirResourceType = CarePlan::class.java

    fun getCarePlansByPatient(
        tenant: Tenant,
        patientFhirId: String,
        category: String,
    ): List<CarePlan> {
        val parameters =
            mapOf(
                "patient" to patientFhirId,
                "category" to category,
            )
        return getResourceListFromSearch(tenant, parameters)
    }
}

enum class CarePlanCategory(val category: String, val code: String) {
    ONCOLOGY("Oncology", "736378000"),
    LONGITUDINAL("Longitudinal", "38717003"),
    ENCOUNTER_LEVEL("Encounter_Level", "734163000"),
    OUTPATIENT("Outpatient", "736271009"),
    PATIENT_EDUCATION("Patient_Education", "409073007"),
}
