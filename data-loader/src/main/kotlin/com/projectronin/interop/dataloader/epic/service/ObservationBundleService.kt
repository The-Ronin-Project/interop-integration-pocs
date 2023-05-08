package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant

class ObservationBundleService(epicClient: EpicClient) : BaseEpicService<Bundle>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Observation"
    override val fhirResourceType = Bundle::class.java

    fun getObservationBundle(tenant: Tenant, observationFhirId: String): Bundle = searchByID(tenant, observationFhirId)
}
