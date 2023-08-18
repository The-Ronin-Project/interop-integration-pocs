package com.projectronin.interop.dataloader.epic

import com.projectronin.interop.dataloader.base.BaseLoader
import com.projectronin.interop.dataloader.epic.service.MockEpicTenantService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.EpicPatientService
import com.projectronin.interop.ehr.epic.auth.EpicAuthenticationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.vendor.Epic

abstract class BaseEpicDataLoader : BaseLoader() {
    private val epicAuthenticationService = EpicAuthenticationService(httpClient)
    protected val ehrAuthenticationBroker = EHRAuthenticationBroker(listOf(epicAuthenticationService))
    protected val epicClient = EpicClient(httpClient, ehrAuthenticationBroker, datalakePublishService)
    abstract val jira: String
    open val tenantMnemonic: String = ""
    private val tenantService = MockEpicTenantService()
    val tenant by lazy {
        tenantService.getTenant(tenantMnemonic)
    }

    private val patientService = EpicPatientService(epicClient, 5, ehrDataAuthorityClient)

    override fun getPatientsForMRNs(mrns: Set<String>): Map<String, Patient> {
        val paddedMrns = if (tenantMnemonic == "mdaoc") {
            mrns.map { it.padStart(7, '0') }
        } else {
            mrns
        }

        val keys = paddedMrns.filter { it.isNotBlank() }
            .associateWith { mrn ->
                Identifier(
                    system = Uri(tenant.vendorAs<Epic>().patientMRNSystem),
                    value = mrn.asFHIR()
                )
            }
        if (keys.isEmpty()) return emptyMap()

        logger.info { "Loading patients for ${keys.size} MRNs" }
        val patients = patientService.findPatientsById(tenant, keys)
        logger.info { "Done loading patients" }
        return patients
    }
}
