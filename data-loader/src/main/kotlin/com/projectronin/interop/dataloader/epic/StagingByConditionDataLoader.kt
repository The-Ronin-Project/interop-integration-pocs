package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.service.ConditionBundleService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.measureTimeMillis

/*
    If we ever decide to do something like this for real, this should probably be merged with the ConditionDataLoader,
    but for a quick data pull this is fine for now.

    New ask from INFX, instead of writing out a csv file, can we write a separate JSON file for every patient that has
    staging data.  They literally want us to include every bundle with the string "stage": in it to avoid potentially
    missing anything.
 */
class StagingByConditionDataLoader(epicClient: EpicClient) : BaseEpicDataLoader() {
    private val observationConditionService = ConditionBundleService(epicClient)
    override val jira = "Prior to paradigm change"
    override fun main() = TODO("Prior to paradigm change")
    fun load(patientsByMrn: Map<String, Patient>, tenant: Tenant) {
        logger.info { "Loading staging observations through conditions" }
        var totalTime: Long = 0
        patientsByMrn.entries.forEachIndexed { index, (mrn, patient) ->
            val executionTime = measureTimeMillis {
                val run = runCatching {
                    loadAndWriteStagingReports(patient, tenant, mrn)
                }

                if (run.isFailure) {
                    val exception = run.exceptionOrNull()
                    logger.error(exception) { "Error processing $mrn: ${exception?.message}" }
                }
            }

            totalTime += executionTime
            logger.info { "Completed ${index + 1} of ${patientsByMrn.size}. Last took $executionTime ms. Current average: ${totalTime / (index + 1)}" }
        }
        logger.info { "Done loading conditions" }
    }

    private fun loadAndWriteStagingReports(
        patient: Patient,
        tenant: Tenant,
        mrn: String
    ) {
        val bundle = observationConditionService.findConditionsAndObservations(tenant, patient.id!!.value!!)
        writeStagingData(bundle, mrn)
    }

    private fun writeStagingData(
        bundle: Bundle,
        mrn: String
    ) {
        val json = JacksonManager.objectMapper.writeValueAsString(bundle)
        if (json.contains("\"stage\":", true)) {
            BufferedWriter(FileWriter(File("loaded/$mrn.json"))).use { writer ->
                writer.write(json)
            }
        }
    }
}
