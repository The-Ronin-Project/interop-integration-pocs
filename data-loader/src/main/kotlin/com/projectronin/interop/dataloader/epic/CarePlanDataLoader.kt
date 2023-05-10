package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.CarePlanCategory
import com.projectronin.interop.dataloader.epic.service.CarePlanService
import com.projectronin.interop.dataloader.epic.service.RequestGroupService
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import kotlin.system.exitProcess

fun main() {
    CarePlanDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class CarePlanDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1703"
    override val tenantMnemonic = "v7r1eczk"
    private val carePlanService = CarePlanService(epicClient)
    private val requestGroupService = RequestGroupService(epicClient)

    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        logger.info { "Timestamp: $timeStamp" }

        val patientsByMrn = getPatientsForMRNs(getMRNs())

        patientsByMrn.entries.mapIndexed { index, (mrn, patient) ->
            logger.info { "Loading care plans for patient $mrn (${index + 1} of ${patientsByMrn.size})" }
            val carePlans = loadCarePlans(patient)
            if (carePlans.isNotEmpty()) {
                val carePlanFileName = "loaded/${mrn}_careplans.json"
                writeFile(carePlanFileName, carePlans)
                uploadFile(carePlanFileName, tenant, "careplans", timeStamp)

                logger.info { "Loading request groups for patient $mrn" }
                val requestGroups = loadRequestGroups(carePlans)
                if (requestGroups.isNotEmpty()) {
                    val requestGroupFileName = "loaded/${mrn}_requestgroups.json"
                    writeFile(requestGroupFileName, requestGroups)
                    uploadFile(requestGroupFileName, tenant, "requestgroups", timeStamp)
                }
            }
        }
    }

    private fun loadCarePlans(
        patient: Patient
    ): List<CarePlan> {
        val carePlans = carePlanService.getCarePlansByPatient(tenant, patient.id!!.value!!, CarePlanCategory.ONCOLOGY.code)

        val cycleCarePlans = carePlans.map { carePlan ->
            carePlan.activity.map { activity ->
                val carePlanReferences = activity.extension.getReferences("CarePlan")

                carePlanReferences.mapNotNull { reference ->
                    val cycleCarePlanId = reference.decomposedId()
                    cycleCarePlanId?.let {
                        carePlanService.getByID(tenant, it)
                    }
                }
            }.flatten()
        }.flatten()

        return carePlans + cycleCarePlans
    }

    private fun loadRequestGroups(carePlans: List<CarePlan>): List<RequestGroup> {
        val requestGroupIds = carePlans.map { carePlan ->
            carePlan.activity.mapNotNull { it.reference }
                .filter { it.decomposedType() == "RequestGroup" }
                .mapNotNull { it.decomposedId() }
        }.flatten()

        return requestGroupIds.map { id ->
            requestGroupService.getByID(tenant, id)
        }
    }

    // Might be worth adding this interop-fhir at some point
    fun List<Extension>.getReferences(referenceType: String? = null): List<Reference> {
        val references = this.mapNotNull { extension ->
            extension.value?.let {
                if (it.type == DynamicValueType.REFERENCE) {
                    it.value as Reference
                } else {
                    null
                }
            }
        }
        return referenceType?.let {
            references.filter { reference -> reference.decomposedType() == referenceType }
        } ?: references
    }
}
