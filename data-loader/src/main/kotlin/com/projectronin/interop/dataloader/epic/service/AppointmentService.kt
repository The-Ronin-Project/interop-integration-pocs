package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AppointmentService(epicClient: EpicClient) : BaseEpicService<Appointment>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Appointment"
    override val fhirResourceType = Appointment::class.java
    val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    val urlPart = "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"

    fun getEpicAppointmentsFromDepartmentIdentifier(tenant: Tenant, departmentIdentifier: Identifier): List<EpicAppointment> {
        val startDate = LocalDate.now()
        val endDate = LocalDate.now()
        val request = GetProviderAppointmentRequest(
            userID = tenant.vendorAs<Epic>().ehrUserId,
            departments = listOf(IDType(id = departmentIdentifier.value!!.value!!, type = "External")),
            startDate = dateFormat.format(startDate),
            endDate = dateFormat.format(endDate)
        )
        val res = runBlocking {
            val httpResponse = epicClient.post(tenant, urlPart, request)
            httpResponse.body<GetAppointmentsResponse>()
        }
        return res.appointments ?: emptyList()
    }
}
