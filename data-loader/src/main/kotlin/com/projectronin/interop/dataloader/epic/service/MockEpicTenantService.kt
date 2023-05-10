package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk

// some day this should be pulling from the actual db / tenant server, but let's abstract this a bit
class MockEpicTenantService {
    // Values can be found in the customer you want to pull froms tenant config when setting up your environment
    val mrnSystem: String = System.getenv("LOAD_MRN_SYSTEM")
    val loadClientId: String = System.getenv("LOAD_CLIENT_ID")
    val loadServiceEndpoint: String = System.getenv("LOAD_SERVICE_ENDPOINT")
    val loadAuthEndpoint: String = System.getenv("LOAD_AUTH_ENDPOINT")
    val loadPrivateKey: String = System.getenv("LOAD_PRIVATE_KEY")

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "apposnd"
        every { name } returns "apposnd"
        every { vendor } returns mockk<Epic> {
            every { type } returns VendorType.EPIC
            every { hsi } returns null
            every { patientMRNSystem } returns mrnSystem
            every { clientId } returns loadClientId
            every { serviceEndpoint } returns loadServiceEndpoint
            every { authenticationConfig } returns mockk {
                every { authEndpoint } returns loadAuthEndpoint
                every { privateKey } returns loadPrivateKey
            }
            every { ehrUserId } returns "1"
        }
    }

    private val psjProdTenant = mockk<Tenant> {
        every { mnemonic } returns "v7r1eczk"
        every { name } returns "psj"
        every { vendor } returns mockk<Epic> {
            every { type } returns VendorType.EPIC
            every { hsi } returns null
            every { patientMRNSystem } returns mrnSystem
            every { clientId } returns loadClientId
            every { serviceEndpoint } returns loadServiceEndpoint
            every { authenticationConfig } returns mockk {
                every { authEndpoint } returns loadAuthEndpoint
                every { privateKey } returns loadPrivateKey
            }
            every { ehrUserId } returns "1"
        }
    }

    private fun getTenants(): List<Tenant> {
        return listOf(tenant, psjProdTenant)
    }

    fun getTenant(tenantMnemonic: String): Tenant {
        return getTenants().first { it.mnemonic == tenantMnemonic }
    }
}
