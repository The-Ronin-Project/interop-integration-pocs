package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant

class ObservationViaConditionService(epicClient: EpicClient) :
    BaseEpicService<Observation>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Condition"
    override val fhirResourceType = Observation::class.java

    fun findObservationsByCondition(
        tenant: Tenant,
        conditionId: String,
    ): List<Observation> {
        val parameters =
            mapOf(
                "_id" to conditionId,
                "_include" to "Condition:assessment",
            )
        return getResourceListFromSearch(tenant, parameters)
    }
}
