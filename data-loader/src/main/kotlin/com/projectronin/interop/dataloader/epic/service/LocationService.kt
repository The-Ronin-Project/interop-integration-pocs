package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant

class LocationService(epicClient: EpicClient) :
    BaseEpicService<Location>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Location"
    override val fhirResourceType = Location::class.java

    fun getDepartmentIdentifierFromFhirID(
        tenant: Tenant,
        fhirID: String,
    ): Identifier? {
        val location = getByID(tenant, fhirID)
        return location.identifier.firstOrNull()
    }
}
