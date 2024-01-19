package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.tenant.config.model.Tenant

class DiagnosticReportService(epicClient: EpicClient) : BaseEpicService<DiagnosticReport>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DiagnosticReport"
    override val fhirResourceType = DiagnosticReport::class.java

    fun getDiagnosticReportsByPatient(
        tenant: Tenant,
        patientFhirId: String,
    ): List<DiagnosticReport> {
        val parameters = mapOf("patient" to patientFhirId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
