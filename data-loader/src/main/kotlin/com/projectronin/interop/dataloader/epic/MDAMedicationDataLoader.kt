package com.projectronin.interop.dataloader.epic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.ehr.epic.EpicEncounterService
import com.projectronin.interop.ehr.epic.EpicMedicationRequestService
import com.projectronin.interop.ehr.epic.EpicMedicationService
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main() {
    MDAMedicationDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class MDAMedicationDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1791"
    override val tenantMnemonic = "mdaoc"
    private val medicationRequestService = EpicMedicationRequestService(epicClient)
    private val encounterService = EpicEncounterService(epicClient)
    private val medicationService = EpicMedicationService(epicClient, batchSize = 5)
    override fun main() {
        val patientsByMrn = getPatientsForMRNs(getMRNs())
        val timeStamp = System.currentTimeMillis().toString()
        logger.info { "Loading Encounters" }
        patientsByMrn.forEach { entry ->
            val patient = entry.value
            val mrn = entry.key
            val fhirId = patient.id?.value!!
            logger.info { "Loading Medications for $mrn" }
            val run = runCatching {
                // discover medication references from patients
                val requests = medicationRequestService.getMedicationRequestByPatient(
                    tenant,
                    fhirId
                )
                logger.info { "Found ${requests.size} requests" }
                val patientID =
                    entry.value.identifier.find { identifier -> identifier.type?.text?.value?.lowercase() == "external" }?.value?.value
                        ?: return@forEach
                val requestsWithAdminDate = mutableListOf<MedicationRequest>()
                requests.map {
                    // logger.info { "MedRequestID:  ${it.id}" }
                    val encounter = kotlin.runCatching {
                        it.encounter?.decomposedId()?.let { encId -> encounterService.getByID(tenant, encId) }
                            ?: return@map
                    }.getOrNull() ?: return@map
                    val contactID =
                        encounter.identifier.find { identifier -> identifier.system?.value == "urn:oid:1.2.840.114350.1.13.412.2.7.3.698084.8" }?.value?.value
                            ?: return@map
                    val orderID =
                        it.identifier.find { identifier -> identifier.system?.value == "urn:oid:1.2.840.114350.1.13.412.2.7.2.798268" }?.value?.value
                            ?: return@map
                    val request = EpicMedAdminRequest(
                        patientID,
                        "External",
                        contactID,
                        "CSN",
                        listOf(OrderID(orderID, "External"))
                    )
                    // logger.info { "MedAdminRequest: $request" }
                    val response = runBlocking {
                        val post = epicClient.post(
                            tenant,
                            "/api/epic/2014/Clinical/Patient/GETMEDICATIONADMINISTRATIONHISTORY/MedicationAdministration",
                            request
                        )
                        // logger.info { post.httpResponse }
                        post.body<EpicMedAdmin>()
                    }
                    // logger.info { response }
                    val admins =
                        response.orders.firstOrNull()?.medicationAdministrations?.filter { admin -> admin.action == "Given" }
                    if (admins != null && admins.isNotEmpty()) {
                        logger.info { "MedAdminRequest: $request" }
                        logger.info { "Admin: $admins" }

                        requestsWithAdminDate.add(
                            it.copy(
                                extension = it.extension + admins.map { admin ->
                                    Extension(
                                        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/medicationRequestAdministrationDate"),
                                        value = DynamicValue(DynamicValueType.DATE_TIME, admin.administrationInstant)
                                    )
                                }
                            )
                        )
                    }
                }
                requestsWithAdminDate.forEach { requestWithAdminDate ->
                    writeAndUploadResources(tenant, requestWithAdminDate.id!!.value!!, listOf(requestWithAdminDate), timeStamp, dryRun = false)
                }
            }

            if (run.isFailure) {
                val exception = run.exceptionOrNull()
                logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
            }
        }
    }
}

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedAdmin(
    val orders: List<MedicationOrder> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class MedicationOrder(
    val medicationAdministrations: List<MedicationAdministration> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class MedicationAdministration(
    val administrationInstant: String,
    val action: String
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedAdminRequest(
    @JsonProperty("PatientID")
    val patientID: String,
    @JsonProperty("PatientIDType")
    val patientIDType: String,
    @JsonProperty("ContactID")
    val contactID: String,
    @JsonProperty("ContactIDType")
    val contactIDType: String,
    @JsonProperty("OrderIDs")
    val orderIDs: List<OrderID>
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class OrderID(
    @JsonProperty("ID")
    val ID: String,
    val type: String
)
