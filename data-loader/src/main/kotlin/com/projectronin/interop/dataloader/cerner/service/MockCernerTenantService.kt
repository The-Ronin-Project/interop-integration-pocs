package com.projectronin.interop.dataloader.cerner.service

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.mockk.every
import io.mockk.mockk

@Suppress("ktlint:standard:max-line-length")
class MockCernerTenantService {
    // Values can be found in the prod proxy server
    val loadAccountId: String = System.getenv("LOAD_ACCOUNT_ID")
    val loadSecret: String = System.getenv("LOAD_SECRET")

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "ejh3j95h"
            every { name } returns "cerncode"
            every { vendor } returns
                mockk<Cerner> {
                    every { type } returns VendorType.CERNER
                    every { serviceEndpoint } returns "https://fhir-ehr-code.cerner.com/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d"
                    every { authenticationConfig } returns
                        mockk {
                            every {
                                authEndpoint
                            } returns "https://authorization.cerner.com/tenants/ec2458f2-1e24-41c8-b71b-0e701af7583d/protocols/oauth2/profiles/smart-v1/token"
                            every { accountId } returns loadAccountId
                            every { secret } returns loadSecret
                        }
                    every { patientMRNSystem } returns "urn:oid:2.16.840.1.113883.6.1000"
                }
        }

    private fun getTenants(): List<Tenant> {
        return listOf(tenant)
    }

    fun getTenant(tenantMnemonic: String): Tenant {
        return getTenants().first { it.mnemonic == tenantMnemonic }
    }
}
