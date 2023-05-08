package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.tenant.config.model.Tenant

class DocumentReferenceService(epicClient: EpicClient) : BaseEpicService<DocumentReference>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DocumentReference"
    override val fhirResourceType = DocumentReference::class.java

    fun getDocumentReferences(tenant: Tenant, patientFhirId: String, category: String? = null): List<DocumentReference> {
        val parameters = buildMap {
            put("patient", patientFhirId)
            category?.let { put("category", category) }
        }

        return getResourceListFromSearch(tenant, parameters)
    }
}
