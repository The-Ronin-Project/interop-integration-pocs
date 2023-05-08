package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant

class DiagnosticReportBundleService(epicClient: EpicClient) : BaseEpicService<Bundle>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DiagnosticReport"
    override val fhirResourceType = Bundle::class.java

    fun getDiagnosticReportsByPatient(tenant: Tenant, patientFhirId: String): Bundle {
        val parameters = mapOf(
            "patient" to patientFhirId
        )
        return getBundleWithPaging(tenant, parameters)
    }
}
