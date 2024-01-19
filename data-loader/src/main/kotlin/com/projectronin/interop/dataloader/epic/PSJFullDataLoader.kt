package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.epic.service.BinaryService
import com.projectronin.interop.dataloader.epic.service.CarePlanCategory
import com.projectronin.interop.dataloader.epic.service.CarePlanService
import com.projectronin.interop.dataloader.epic.service.DocumentReferenceService
import com.projectronin.interop.dataloader.epic.service.ImmunizationService
import com.projectronin.interop.dataloader.epic.service.ObservationService
import com.projectronin.interop.dataloader.epic.service.ObservationViaConditionService
import com.projectronin.interop.dataloader.epic.service.RequestGroupService
import com.projectronin.interop.ehr.epic.EpicConditionService
import com.projectronin.interop.ehr.epic.EpicEncounterService
import com.projectronin.interop.ehr.epic.EpicMedicationRequestService
import com.projectronin.interop.ehr.epic.EpicMedicationService
import com.projectronin.interop.ehr.epic.EpicMedicationStatementService
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.resource.Immunization
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.r4.resource.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDate
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main() {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    PSJFullDataLoader().main()
    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class PSJFullDataLoader : BaseEpicDataLoader() {
    override val jira = "INT-1881"
    override val tenantMnemonic = "v7r1eczk"

    private val dryRun = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val parallelDispatcher = Dispatchers.Default.limitedParallelism(20)

    private val timestamp = System.currentTimeMillis().toString()
    private val today = LocalDate.now()
    private val startDate = today.minusDays(60)
    private val endDate = today

    private val carePlanService = CarePlanService(epicClient)
    private val requestGroupService = RequestGroupService(epicClient)
    private val medicationService = EpicMedicationService(epicClient, 5)
    private val medicationRequestService = EpicMedicationRequestService(epicClient)
    private val medicationStatementService = EpicMedicationStatementService(epicClient)

    private val immunizationService = ImmunizationService(epicClient)

    private val encounterService = EpicEncounterService(epicClient)
    private val conditionService = EpicConditionService(epicClient)
    private val observationService = ObservationService(epicClient)
    private val observationForConditionService = ObservationViaConditionService(epicClient)

    private val documentReferenceService = DocumentReferenceService(epicClient)
    private val binaryService = BinaryService(epicClient, ehrAuthenticationBroker, httpClient)

    override fun main() {
        val duration =
            measureTimeMillis {
                val patientsByMrn = loadPatients()
                val totalPatients = patientsByMrn.size

                val loadedMedicationIds = mutableSetOf<String>()

                patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                    uploadResource(mrn, patient)

                    logger.info { "Loading CarePlans for patient ${index + 1} of $totalPatients" }
                    val carePlans = loadCarePlans(patient)
                    uploadResources(mrn, carePlans)

                    logger.info { "Loading RequestGroups for patient ${index + 1} of $totalPatients" }
                    val requestGroups = loadRequestGroups(carePlans)
                    uploadResources(mrn, requestGroups)

                    logger.info { "Loading MedicationRequests for patient ${index + 1} of $totalPatients" }
                    val medicationRequests = loadMedicationRequests(patient)
                    uploadResources(mrn, medicationRequests)

                    logger.info { "Loading MedicationStatements for patient ${index + 1} of $totalPatients" }
                    val medicationStatements = loadMedicationStatements(patient)
                    uploadResources(mrn, medicationStatements)

                    logger.info { "Loading Medications for patient ${index + 1} of $totalPatients" }
                    val medications = loadMedications(medicationStatements, medicationRequests, loadedMedicationIds)
                    medications.forEach { uploadResource(it.id!!.value!!, it) }

                    logger.info { "Loading Immunizations for patient ${index + 1} of $totalPatients" }
                    val immunizations = loadImmunizations(patient)
                    uploadResources(mrn, immunizations)

                    logger.info { "Loading Encounters for patient ${index + 1} of $totalPatients" }
                    val encounters = loadEncounters(patient)
                    uploadResources(mrn, encounters)

                    logger.info { "Loading Conditions for patient ${index + 1} of $totalPatients" }
                    val conditions = loadConditions(patient)
                    uploadResources(mrn, conditions)

                    logger.info { "Loading Observations for patient ${index + 1} of $totalPatients" }
                    val observations = loadObservations(patient, conditions)
                    uploadResources(mrn, observations)

                    logger.info { "Loading DocumentReferences for patient ${index + 1} of $totalPatients" }
                    val documentReferences = loadDocumentReferences(patient)
                    uploadResources(mrn, documentReferences)

                    logger.info { "Loading Binaries for patient ${index + 1} of $totalPatients" }
                    loadBinaries(documentReferences)

                    logger.info { "Completed patient ${index + 1} of $totalPatients" }
                    logger.info { "" }
                }
            }
        logger.info { "Completed load in ${Duration.ofMillis(duration)}" }
    }

    private fun <R : Resource<*>> uploadResource(
        key: String,
        value: R,
    ) {
        uploadResources(key, listOf(value))
    }

    private fun <R : Resource<*>> uploadResources(
        key: String,
        value: Collection<R>,
    ) {
        writeAndUploadResources(
            tenant,
            key,
            value.toList(),
            timestamp,
            dryRun = dryRun,
        )
    }

    private fun loadPatients(): Map<String, Patient> {
        logger.info { "Loading Patients" }
        val patientsByMrn = getPatientsForMRNs(getMRNs())
        logger.info { "Done loading Patients" }
        return patientsByMrn
    }

    private fun loadCarePlans(patient: Patient): List<CarePlan> {
        val carePlans =
            carePlanService.getCarePlansByPatient(tenant, patient.id!!.value!!, CarePlanCategory.ONCOLOGY.code)

        val cycleCarePlans =
            carePlans.map { carePlan ->
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

    private fun loadMedicationRequests(patient: Patient): List<MedicationRequest> {
        return medicationRequestService.getMedicationRequestByPatient(tenant, patient.id!!.value!!)
    }

    private fun loadMedicationStatements(patient: Patient): List<MedicationStatement> {
        return medicationStatementService.getMedicationStatementsByPatientFHIRId(tenant, patient.id!!.value!!)
    }

    private fun loadMedications(
        medicationStatements: List<MedicationStatement>,
        medicationRequests: List<MedicationRequest>,
        loadedMedicationIds: MutableSet<String>,
    ): List<Medication> {
        logger.info { "Loading Medications" }

        val medicationStatementMedicationIds =
            medicationStatements.mapNotNull {
                val medication = it.medication!!
                if (medication.type == DynamicValueType.REFERENCE) {
                    (medication.value as Reference).decomposedId()
                } else {
                    null
                }
            }.toSet()

        val medicationRequestMedicationIds =
            medicationRequests.mapNotNull {
                val medication = it.medication!!
                if (medication.type == DynamicValueType.REFERENCE) {
                    (medication.value as Reference).decomposedId()
                } else {
                    null
                }
            }.toSet()

        val medicationIds = medicationRequestMedicationIds + medicationStatementMedicationIds - loadedMedicationIds
        val medications = medicationService.getMedicationsByFhirId(tenant, medicationIds.toList())

        loadedMedicationIds.addAll(medicationIds)
        return medications
    }

    private fun loadImmunizations(patient: Patient): List<Immunization> {
        return immunizationService.getImmunizationsForPatient(tenant, patient.id!!.value!!)
    }

    private fun loadEncounters(patient: Patient): List<Encounter> {
        return encounterService.findPatientEncounters(tenant, patient.id!!.value!!, startDate, endDate)
    }

    private fun loadConditions(patient: Patient): List<Condition> {
        return loadConditions(patient, "encounter-diagnosis") + loadConditions(patient, "problem-list-item")
    }

    private fun loadConditions(
        patient: Patient,
        category: String,
    ): List<Condition> {
        return conditionService.findConditions(tenant, patient.id!!.value!!, category, "active")
    }

    private fun loadObservations(
        patient: Patient,
        conditions: List<Condition>,
    ): Set<Observation> {
        val observationCategories =
            setOf(
                "genomics",
                "laboratory",
                "social-history",
                "functional-mental-status",
                "http://snomed.info/sct|384821006",
                "http://snomed.info/sct|118228005",
                "smartdata",
                "vital-signs",
            )
        val categoryBasedObservations = observationCategories.flatMap { loadObservations(patient, it) }
        val conditionBasedObservations =
            conditions.flatMap {
                observationForConditionService.findObservationsByCondition(tenant, it.id!!.value!!)
            }
        return (categoryBasedObservations + conditionBasedObservations).toSet()
    }

    private fun loadObservations(
        patient: Patient,
        category: String,
    ): List<Observation> {
        return observationService.findObservationsByPatient(
            tenant,
            listOf(patient.id!!.value!!),
            listOf(category),
            startDate,
        )
    }

    private fun loadDocumentReferences(patient: Patient): List<DocumentReference> {
        return documentReferenceService.getDocumentReferences(tenant, patient.id!!.value!!)
    }

    private fun loadBinaries(documentReferences: List<DocumentReference>) {
        logger.info { "Loading binaries for ${documentReferences.size} document references" }

        runCatching { Paths.get("loaded/binary").createDirectories() }

        runBlocking {
            withContext(coroutineContext) {
                documentReferences.forEach { documentReference ->
                    documentReference.content.forEach { content ->
                        val attachment = content.attachment!!
                        attachment.url?.value?.let {
                            if (it.startsWith("Binary/")) {
                                when (attachment.contentType?.value) {
                                    "text/plain" -> "txt"
                                    "text/html" -> "html"
                                    "application/pdf" -> "pdf"
                                    "text/rtf" -> "rtf"
                                    "image/jpeg" -> "jpg"
                                    "application/octet-stream" -> null // Not totally sure what to do with this one
                                    else -> null
                                }?.let { extension ->
                                    val binaryFhirId = it.removePrefix("Binary/")
                                    async(parallelDispatcher) {
                                        loadBinary(binaryFhirId, extension)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadBinary(
        binaryFhirId: String,
        extension: String,
    ) {
        val binaryFileName = "loaded/binary/$binaryFhirId.$extension"
        val file = File(binaryFileName)
        if (listOf("pdf", "jpg").contains(extension)) {
            binaryService.getBinary(tenant, binaryFhirId)?.let { binary ->
                val binaryBytes = Base64.getDecoder().decode(binary.data!!.value!!)
                FileOutputStream(file).use { writer ->
                    writer.write(binaryBytes)
                }
            }
        } else {
            val binary = binaryService.getBinaryData(tenant, binaryFhirId)
            BufferedWriter(FileWriter(file)).use { writer ->
                writer.write(binary)
            }
        }

        if (file.exists() && !dryRun) {
            expOCIClient.uploadExport(tenant, "binary", binaryFileName, timestamp)
        }
    }

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
