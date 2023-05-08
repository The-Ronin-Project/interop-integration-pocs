package com.projectronin.interop.dataloader.epic.service

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking

class StagingReportService(private val epicClient: EpicClient) {
    private val stagingSearchUrlPart = "/api/epic/2018/Clinical/Oncology/GetPatientStagingDataEpic/Clinical/Oncology/Staging/GetPatientData/false"

    fun getStagingReportsByPatient(tenant: Tenant, mrn: String): GetPatientStagingResponse {
        val request = GetPatientStagingRequest(
            signedOnly = false,
            patientId = IDType(
                id = mrn,
                type = "ORCA MRN"
            ),
            auditUserId = IDType(
                id = "",
                type = ""
            )
        )

        return runBlocking {
            val httpResponse = epicClient.post(tenant, stagingSearchUrlPart, request)
            httpResponse.body()
        }
    }
}

data class GetPatientStagingRequest(
    val signedOnly: Boolean? = false,
    val patientId: IDType,
    val auditUserId: IDType
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetPatientStagingResponse(
    val stages: List<PatientStage> = listOf()
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class PatientStage(
    val classification: String?,
    val problemDescription: String,
    val problemID: String,
    val stagingMethod: String?,
    val tValue: String?,
    val nValue: String?,
    val mValue: String?,
    val stageGroup: String?,
    val stageModifier: String?,
    val eRStatus: String?,
    val pRStatus: String?,
    val hER2Status: String?,
    val stageDate: String?,
    val signStatus: Boolean?,
    val diagnosisDate: String?,
    val diagnosisCodeSet: String?,
    val diagnosisCode: String?,
    val histologicGrade: HistologicGrade?
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class HistologicGrade(
    val grade: String?,
    val system: String?,
    val method: String?
)
