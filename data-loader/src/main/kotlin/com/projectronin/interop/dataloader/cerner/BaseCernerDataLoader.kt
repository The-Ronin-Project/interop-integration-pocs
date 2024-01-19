package com.projectronin.interop.dataloader.cerner

import com.projectronin.interop.dataloader.base.BaseLoader
import com.projectronin.interop.dataloader.cerner.service.MockCernerTenantService
import com.projectronin.interop.dataloader.cerner.service.authentication.CernerAuthenticationService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.cerner.CernerPatientService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.vendor.Epic

abstract class BaseCernerDataLoader : BaseLoader() {
    private val cernerAuthenticationService = CernerAuthenticationService(httpClient)

    private val ehrAuthenticationBroker = EHRAuthenticationBroker(listOf(cernerAuthenticationService))
    protected val cernerClient = CernerClient(httpClient, ehrAuthenticationBroker, datalakePublishService)
    abstract val jira: String
    abstract val tenantMnemonic: String
    private val tenantService = MockCernerTenantService()
    val tenant by lazy { tenantService.getTenant(tenantMnemonic) }

    private val patientService = CernerPatientService(cernerClient, ehrDataAuthorityClient)

    override fun getPatientsForMRNs(mrns: Set<String>): Map<String, Patient> {
        val keys =
            mrns.filter { it.isNotBlank() }
                .associateWith { mrn ->
                    Identifier(
                        system = Uri(tenant.vendorAs<Epic>().patientMRNSystem),
                        value = mrn.asFHIR(),
                    )
                }
        if (keys.isEmpty()) return emptyMap()

        logger.info { "Loading patients for ${keys.size} MRNs" }
        val patients = patientService.findPatientsById(tenant, keys)
        logger.info { "Done loading patients" }
        return patients
    }
}
