package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.dataloader.epic.resource.ConditionDataLoader
import com.projectronin.interop.dataloader.epic.resource.MedicationDataLoader
import com.projectronin.interop.dataloader.epic.resource.ObservationDataLoader
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.EpicPatientService
import com.projectronin.interop.ehr.epic.auth.EpicAuthenticationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk
import mu.KotlinLogging
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService

val mrnSystem: String = System.getenv("LOAD_MRN_SYSTEM")
val loadClientId: String = System.getenv("LOAD_CLIENT_ID")
val loadServiceEndpoint: String = System.getenv("LOAD_SERVICE_ENDPOINT")
val loadAuthEndpoint: String = System.getenv("LOAD_AUTH_ENDPOINT")
val loadPrivateKey: String = System.getenv("LOAD_PRIVATE_KEY")

fun main() {
    EpicDataLoader().load()

    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class EpicDataLoader {
    private val logger = KotlinLogging.logger { }
    private val httpClient = HttpSpringConfig().getHttpClient()

    private val epicAuthenticationService = EpicAuthenticationService(httpClient)
    private val ehrAuthenticationBroker = EHRAuthenticationBroker(listOf(epicAuthenticationService))
    private val epicClient = EpicClient(httpClient, ehrAuthenticationBroker)

    private val aidboxPatientService = mockk<AidboxPatientService>(relaxed = true)
    private val patientService = EpicPatientService(epicClient, 5, aidboxPatientService)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "psj-prod"
        every { name } returns "PSJ"
        every { vendor } returns mockk<Epic> {
            every { type } returns VendorType.EPIC
            every { hsi } returns null
            every { clientId } returns loadClientId
            every { serviceEndpoint } returns loadServiceEndpoint
            every { authenticationConfig } returns mockk {
                every { authEndpoint } returns loadAuthEndpoint
                every { privateKey } returns loadPrivateKey
            }
        }
    }

    fun load() {
        runCatching { Paths.get("loaded").createDirectory() }

        val patientsByMRN = getPatientsForMRNs(getMRNs())
        ConditionDataLoader(epicClient).load(patientsByMRN, tenant, "loaded/conditions.csv")
        ObservationDataLoader(epicClient).load(
            patientsByMRN,
            tenant,
            LocalDate.of(2022, 1, 1),
            "loaded/observations.csv"
        )
        MedicationDataLoader(epicClient).load(patientsByMRN, tenant, "loaded/medications.csv")

        // This one is focused on Genomics specifically.
        ObservationDataLoader(epicClient).load(
            patientsByMRN,
            tenant,
            null,
            "loaded/genomics.csv",
            listOf("genomics")
        )
    }

    private fun getMRNs(): Set<String> =
        this.javaClass.getResource("/mrns.txt")!!.readText().split("\r\n", "\n").toSet()

    private fun getPatientsForMRNs(mrns: Set<String>): Map<String, Patient> {
        val keys = mrns.filter { it.isNotBlank() }
            .associateWith { mrn -> Identifier(system = Uri(mrnSystem), value = mrn.asFHIR()) }
        if (keys.isEmpty()) return emptyMap()

        logger.info { "Loading patients for ${keys.size} MRNs" }
        val patients = patientService.findPatientsById(tenant, keys)
        logger.info { "Done loading patients" }
        return patients
    }
}
