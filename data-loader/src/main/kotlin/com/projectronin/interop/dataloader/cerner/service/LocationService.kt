package com.projectronin.interop.dataloader.cerner.service

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant

class LocationService(cernerClient: CernerClient) : BaseCernerService<Location>(cernerClient) {
    override val fhirURLSearchPart = "/Location"
    override val fhirResourceType = Location::class.java

    fun getSomeLocations(tenant: Tenant): List<Location> {
        val parameters = mapOf(
            "address-state" to "MO"
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
