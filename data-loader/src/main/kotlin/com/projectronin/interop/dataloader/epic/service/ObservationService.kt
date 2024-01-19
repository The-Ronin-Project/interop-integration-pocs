package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.ehr.util.toSearchTokens
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

class ObservationService(epicClient: EpicClient) :
    BaseEpicService<Observation>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Observation"
    override val fhirResourceType = Observation::class.java

    fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<String>,
        startDate: LocalDate?,
    ): List<Observation> {
        val observationResponses =
            patientFhirIds.chunked(1) {
                val parameters =
                    mapOf(
                        "patient" to it.joinToString(separator = ","),
                        "category" to observationCategoryCodes.toSearchTokens().toOrParams(),
                    ) + if (startDate == null) emptyMap() else mapOf("date" to "ge$startDate")
                getResourceListFromSearch(tenant, parameters)
            }
        return observationResponses.flatten()
    }
}
