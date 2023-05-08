package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant

class MedicationRequestService(epicClient: EpicClient) :
    BaseEpicService<MedicationRequest>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/MedicationRequest"
    override val fhirResourceType = MedicationRequest::class.java

    fun getMedicationRequestsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String
    ): List<MedicationRequest> {
        val parameters = mapOf("patient" to patientFHIRId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
