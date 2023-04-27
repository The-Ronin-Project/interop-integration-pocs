package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.dataloader.epic.resource.DocumentReferenceDataLoader
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
import kotlin.io.path.createDirectory
import kotlin.system.exitProcess
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService

// Values can be found in the customer you want to pull froms tenant config when setting up your environment
val mrnSystem: String = System.getenv("LOAD_MRN_SYSTEM")
val loadClientId: String = System.getenv("LOAD_CLIENT_ID")
val loadServiceEndpoint: String = System.getenv("LOAD_SERVICE_ENDPOINT")
val loadAuthEndpoint: String = System.getenv("LOAD_AUTH_ENDPOINT")
val loadPrivateKey: String = System.getenv("LOAD_PRIVATE_KEY")

// Values can be found in Vault mirth-connector when setting up your environment
val namespace: String = System.getenv("LOAD_OCI_NAMESPACE")
val tenancyOCID: String = System.getenv("LOAD_OCI_TENANCY_OCID")
val userOCID: String = System.getenv("LOAD_OCI_USER_OCID")
val fingerPrint: String = System.getenv("LOAD_OCI_FINGERPRINT")
val regionId: String = System.getenv("LOAD_OCI_REGION_ID")
val privateKey: String = System.getenv("LOAD_OCI_PRIVATE_KEY")

fun main() {
    EpicDataLoader().load()

    // This is wanting to hang here on my machine for some reason, so we force the exit.
    exitProcess(1)
}

class EpicDataLoader {
    private val logger = KotlinLogging.logger { }
    private val httpClient = HttpSpringConfig().getHttpClient()
    private val expClient = ExperimentationOCIClient(
        tenancyOCID = tenancyOCID,
        userOCID = userOCID,
        fingerPrint = fingerPrint,
        privateKey = privateKey,
        namespace = namespace,
        regionId = regionId
    )

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
        val timeStamp = System.currentTimeMillis().toString()
        runCatching { Paths.get("loaded").createDirectory() }

        val patientsByMRN = getPatientsForMRNs(getMRNs())

        DocumentReferenceDataLoader(epicClient, ehrAuthenticationBroker, httpClient, expClient).load(
            patientsByMRN,
            tenant,
            timeStamp
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
