package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant

class ConditionBundleService(epicClient: EpicClient) :
    BaseEpicService<Bundle>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Condition"
    override val fhirResourceType = Bundle::class.java

    fun findConditionsAndObservations(
        tenant: Tenant,
        patientFhirId: String
    ): Bundle {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "category" to "problem-list-item",
            "clinical-status" to "active",
            "_include" to "Condition:Observation"
        )

        return getBundleWithPaging(tenant, parameters)
    }
}
