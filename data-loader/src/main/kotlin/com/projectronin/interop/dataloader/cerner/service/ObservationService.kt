package com.projectronin.interop.dataloader.cerner.service

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.time.LocalDate

class ObservationService(cernerClient: CernerClient) : BaseCernerService<Observation>(cernerClient) {
    override val fhirURLSearchPart = "/Observation"
    override val fhirResourceType = Observation::class.java
    private val logger = KotlinLogging.logger { }

    fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        startDate: LocalDate?,
    ): List<Observation> {
        val observationResponses =
            patientFhirIds.chunked(1) {
                logger.info { "finding observations for patient $it" }
                val parameters =
                    mapOf(
                        "patient" to it.joinToString(separator = ","),
                    ) + if (startDate == null) emptyMap() else mapOf("date" to "ge$startDate")
                val results = getResourceListFromSearch(tenant, parameters)
                logger.info { "found ${results.size}" }
                results
            }
        return observationResponses.flatten()
    }
}
