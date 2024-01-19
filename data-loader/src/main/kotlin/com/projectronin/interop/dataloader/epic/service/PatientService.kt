package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant

class PatientService(epicClient: EpicClient) : BaseEpicService<Patient>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Patient"
    override val fhirResourceType = Patient::class.java

    fun patientsFromIdentifiers(
        tenant: Tenant,
        patientIdents: List<Identifier>,
    ): List<Patient> {
        val results =
            patientIdents.chunked(10) {
                val identifierParam =
                    it.joinToString(separator = ",") { patientIdentifier ->
                        "${patientIdentifier.system?.value}|${patientIdentifier.value!!.value}"
                    }
                getResourceListFromSearch(
                    tenant,
                    mapOf("identifier" to identifierParam),
                )
            }.flatten()
        return results
    }
}
