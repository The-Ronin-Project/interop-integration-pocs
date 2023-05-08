package com.projectronin.interop.dataloader.cerner.service

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.mockk.every
import io.mockk.mockk

class MockCernerTenantService {
    // Values can be found in the customer you want to pull froms tenant config when setting up your environment
    val mrnSystem: String = System.getenv("LOAD_MRN_SYSTEM")
    val loadAccountId: String = System.getenv("LOAD_ACCOUNT_ID")
    val loadServiceEndpoint: String = System.getenv("LOAD_SERVICE_ENDPOINT")
    val loadAuthEndpoint: String = System.getenv("LOAD_AUTH_ENDPOINT")
    val loadSecret: String = System.getenv("LOAD_SECRET")

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "cerncode"
        every { name } returns "cerncode"
        every { vendor } returns mockk<Cerner> {
            every { type } returns VendorType.CERNER
            every { serviceEndpoint } returns loadServiceEndpoint
            every { authenticationConfig } returns mockk {
                every { authEndpoint } returns loadAuthEndpoint
                every { accountId } returns loadAccountId
                every { secret } returns loadSecret
            }
            every { patientMRNSystem } returns mrnSystem
        }
    }

    private fun getTenants(): List<Tenant> {
        return listOf(tenant)
    }

    fun getTenant(tenantMnemonic: String): Tenant {
        return getTenants().first { it.mnemonic == tenantMnemonic }
    }
}
