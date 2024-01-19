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
    override val jira = "INT-2049" // originally INT-1703
    override val tenantMnemonic = "mdaoc"
    private val carePlanService = CarePlanService(epicClient)
    private val requestGroupService = RequestGroupService(epicClient)

    private val dryRun = false
    private val loadRequestGroups = false

    override fun main() {
        val timeStamp = System.currentTimeMillis().toString()
        logger.info { "Timestamp: $timeStamp" }

        val patientsByMrn = getPatientsForMRNs(getMRNs())

        patientsByMrn.entries.mapIndexed { index, (mrn, patient) ->
            logger.info { "Loading care plans for patient $mrn (${index + 1} of ${patientsByMrn.size})" }
            val carePlans = loadCarePlans(patient)
            if (carePlans.isNotEmpty()) {
                writeAndUploadResources(tenant, mrn, carePlans, timeStamp, dryRun)

                if (loadRequestGroups) {
                    logger.info { "Loading request groups for patient $mrn" }
                    val requestGroups = loadRequestGroups(carePlans)
                    if (requestGroups.isNotEmpty()) {
                        writeAndUploadResources(tenant, mrn, requestGroups, timeStamp, dryRun)
                    }
                }
            }
        }
    }

    private fun loadCarePlans(patient: Patient): List<CarePlan> {
        val carePlans =
            carePlanService.getCarePlansByPatient(tenant, patient.id!!.value!!, CarePlanCategory.ONCOLOGY.code)

        logger.info { "Loaded ${carePlans.size} CarePlans" }
        val carePlanIds = carePlans.map { it.id!!.value!! }.toSet()
        val cycleCarePlanIds = getCycleCarePlanIds(carePlans)
        logger.info { "Found ${cycleCarePlanIds.size} cycle CarePlans to load" }

        return carePlans + loadCarePlans(cycleCarePlanIds, carePlanIds)
    }

    private fun getCycleCarePlanIds(carePlans: Collection<CarePlan>): Set<String> {
        return carePlans.map { carePlan ->
            carePlan.activity.map { activity ->
                val carePlanReferences = activity.extension.getReferences("CarePlan")

                carePlanReferences.mapNotNull { reference ->
                    val cycleCarePlanId = reference.decomposedId()
                    cycleCarePlanId
                }
            }.flatten()
        }.flatten().toSet()
    }

    private fun loadCarePlans(
        requestLoad: Set<String>,
        previouslyLoadedIds: Set<String>,
    ): List<CarePlan> {
        val toLoad = requestLoad - previouslyLoadedIds
        if (toLoad.isEmpty()) {
            return emptyList()
        }

        logger.info { "Loading ${toLoad.size} new CarePlans" }
        val loaded = carePlanService.getByIDs(tenant, toLoad.toList()).values
        logger.info { "Loaded ${loaded.size} CarePlans" }
        if (loaded.isEmpty()) {
            return emptyList()
        }

        val loadedIds = loaded.map { it.id!!.value!! }
        val cycleIds = getCycleCarePlanIds(loaded)
        logger.info { "Found ${cycleIds.size} cycle CarePlans to load" }

        return loaded + loadCarePlans(cycleIds, previouslyLoadedIds + loadedIds)
    }

    private fun loadRequestGroups(carePlans: List<CarePlan>): List<RequestGroup> {
        val requestGroupIds =
            carePlans.map { carePlan ->
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
        val references =
            this.mapNotNull { extension ->
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
