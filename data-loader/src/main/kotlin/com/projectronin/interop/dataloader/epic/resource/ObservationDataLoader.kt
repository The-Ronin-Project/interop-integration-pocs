package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.Code
import com.projectronin.interop.dataloader.epic.resource.service.BaseEpicService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.ehr.util.toSearchTokens
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class ObservationDataLoader(epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }
    private val flushFrequency = 1_000
    private val observationService = EpicDateLimitedObservationService(epicClient)

    fun load(
        patientsByMrn: Map<String, Patient>,
        tenant: Tenant,
        startDate: LocalDate?,
        filename: String = "observations.csv",
        interestedObservations: List<String> = emptyList()
    ) {
        logger.info { "Loading observations" }
        BufferedWriter(FileWriter(File(filename))).use { writer ->
            writer.write(""""MRN","Observation FHIR ID","Category","Code System","Code","Display","Escaped JSON"""")
            writer.newLine()

            var totalTime: Long = 0
            patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
                val executionTime = measureTimeMillis {
                    val run = runCatching {
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "genomics",
                            interestedObservations,
                            startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "laboratory",
                            interestedObservations,
                            startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "social-history",
                            interestedObservations,
                            startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "functional-mental-status",
                            interestedObservations,
                            startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "http://snomed.info/sct|384821006",
                            interestedObservations, startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "http://snomed.info/sct|118228005",
                            interestedObservations, startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "smartdata",
                            interestedObservations,
                            startDate,
                            mrn,
                            writer
                        )
                        loadAndWriteObservations(
                            patient,
                            tenant,
                            "vital-signs",
                            interestedObservations,
                            startDate,
                            mrn,
                            writer
                        )
                    }

                    if (run.isFailure) {
                        val exception = run.exceptionOrNull()
                        logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
                    }
                }

                totalTime += executionTime
                logger.info { "Completed ${index + 1} of ${patientsByMrn.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
            }
        }
        logger.info { "Done loading observations" }
    }

    private fun loadAndWriteObservations(
        patient: Patient,
        tenant: Tenant,
        category: String,
        interestedCategories: List<String>,
        startDate: LocalDate?,
        mrn: String,
        writer: BufferedWriter
    ) {
        if (interestedCategories.isNotEmpty() && !interestedCategories.contains(category)) return

        val observations = getObservationsForPatient(patient, tenant, category, startDate)
        observations.entries.forEachIndexed { index, (key, value) ->
            writeObservation(key, value, category, mrn, writer)

            if (index % flushFrequency == 0) {
                writer.flush()
            }
        }
        logger.info { "Found ${observations.size} observations for category $category" }
    }

    private fun getObservationsForPatient(
        patient: Patient,
        tenant: Tenant,
        category: String,
        startDate: LocalDate?
    ): Map<Code, Observation> =
        observationService.findObservationsByPatient(tenant, listOf(patient.id!!.value!!), listOf(category), startDate)
            .mapNotNull {
                it.code?.coding?.map { coding ->
                    Pair(
                        Code(
                            coding.system?.value ?: "",
                            coding.code?.value ?: "",
                            coding.display?.value ?: ""
                        ), it
                    )
                }
            }.flatten().associate { it.first to it.second }

    private fun writeObservation(
        code: Code,
        observation: Observation,
        category: String,
        mrn: String,
        writer: BufferedWriter
    ) {
        val json = JacksonManager.objectMapper.writeValueAsString(observation)
        val escapedJson = StringEscapeUtils.escapeCsv(json)
        writer.write(""""$mrn","${observation.id!!.value}","$category","${code.system}","${code.code}","${code.display}",$escapedJson""")
        writer.newLine()
    }
}

class EpicDateLimitedObservationService(epicClient: EpicClient) :
    BaseEpicService<Observation>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Observation"
    override val fhirResourceType = Observation::class.java

    fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<String>,
        startDate: LocalDate?
    ): List<Observation> {
        val observationResponses = patientFhirIds.chunked(1) {
            val parameters = mapOf(
                "patient" to it.joinToString(separator = ","),
                "category" to observationCategoryCodes.toSearchTokens().toOrParams()
            ) + if (startDate == null) emptyMap() else mapOf("date" to "ge$startDate")
            getResourceListFromSearch(tenant, parameters)
        }
        return observationResponses.flatten()
    }
}
