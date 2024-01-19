package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Immunization
import com.projectronin.interop.tenant.config.model.Tenant

class ImmunizationService(epicClient: EpicClient) :
    BaseEpicService<Immunization>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Immunization"
    override val fhirResourceType = Immunization::class.java

    fun getImmunizationsForPatient(
        tenant: Tenant,
        patientFHIRId: String,
    ): List<Immunization> {
        val parameters = mapOf("patient" to patientFHIRId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
