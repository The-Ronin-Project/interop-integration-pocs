package com.projectronin.interop.dataloader.cerner.service

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.cerner.client.RepeatingParameter
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate
import java.time.ZoneOffset

class AppointmentService(cernerClient: CernerClient) : BaseCernerService<Appointment>(cernerClient) {
    override val fhirURLSearchPart = "/Appointment"
    override val fhirResourceType = Appointment::class.java

    fun getAppointmentFromLocation(tenant: Tenant, locationFhirID: String, minusDays: Int = 1, plusDays: Int = 1): List<Appointment> {
        val offset = ZoneOffset.UTC
        val startDate = LocalDate.now().minusDays(minusDays.toLong())
        val endDate = startDate.plusDays(plusDays.toLong())
        val parameters = mapOf(
            "location" to locationFhirID,
            "date" to RepeatingParameter(
                listOf(
                    "ge${startDate}T00:00:00$offset",
                    "lt${endDate.plusDays(1)}T00:00:00$offset"
                )
            )
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
