package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk

// some day this should be pulling from the actual db / tenant server, but let's abstract this a bit
class MockEpicTenantService {
    // Values can be found in the prod proxy server
    val loadClientId: String = System.getenv("LOAD_CLIENT_ID")
    val loadPrivateKey: String = System.getenv("LOAD_PRIVATE_KEY")

    private val apposndTenant =
        mockk<Tenant> {
            every { mnemonic } returns "apposnd"
            every { name } returns "apposnd"
            every { vendor } returns
                mockk<Epic> {
                    every { type } returns VendorType.EPIC
                    every { hsi } returns null
                    every { patientMRNSystem } returns "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.1"
                    every { clientId } returns loadClientId
                    every { serviceEndpoint } returns "https://vendorservices.epic.com/interconnect-amcurprd-oauth"
                    every { authenticationConfig } returns
                        mockk {
                            every { authEndpoint } returns "https://vendorservices.epic.com/interconnect-amcurprd-oauth/oauth2/token"
                            every { privateKey } returns loadPrivateKey
                        }
                    every { ehrUserId } returns "1"
                }
        }

    private val psjProdTenant =
        mockk<Tenant> {
            every { mnemonic } returns "v7r1eczk"
            every { name } returns "psj"
            every { vendor } returns
                mockk<Epic> {
                    every { type } returns VendorType.EPIC
                    every { hsi } returns null
                    every { patientMRNSystem } returns "urn:oid:1.2.840.114350.1.13.297.2.7.3.688884.100.2"
                    every { clientId } returns loadClientId
                    every { serviceEndpoint } returns "https://haikuor.providence.org/fhirproxy"
                    every { authenticationConfig } returns
                        mockk {
                            every { authEndpoint } returns "https://haikuor.providence.org/fhirproxy/oauth2/token"
                            every { privateKey } returns loadPrivateKey
                        }
                    every { ehrUserId } returns "1"
                }
        }

    private val mdaProdTenant =
        mockk<Tenant> {
            every { mnemonic } returns "mdaoc"
            every { name } returns "mda"
            every { vendor } returns
                mockk<Epic> {
                    every { type } returns VendorType.EPIC
                    every { hsi } returns null
                    every { patientMRNSystem } returns "urn:oid:1.2.840.114350.1.13.412.2.7.5.737384.14"
                    every { clientId } returns loadClientId
                    every { serviceEndpoint } returns "https://fhir.mdanderson.org/FHIR/"
                    every { authenticationConfig } returns
                        mockk {
                            every { authEndpoint } returns "https://fhir.mdanderson.org/FHIR/oauth2/token"
                            every { privateKey } returns loadPrivateKey
                        }
                    every { ehrUserId } returns "010101"
                }
        }

    private val mdaTstTenant =
        mockk<Tenant> {
            every { mnemonic } returns "mdaoc-stage"
            every { name } returns "mda"
            every { vendor } returns
                mockk<Epic> {
                    every { type } returns VendorType.EPIC
                    every { hsi } returns null
                    every { patientMRNSystem } returns "urn:oid:1.2.840.114350.1.13.412.3.7.5.737384.14"
                    every { clientId } returns loadClientId
                    every { serviceEndpoint } returns "https://fhirtst.mdanderson.org/fhirtst/"
                    every { authenticationConfig } returns
                        mockk {
                            every { authEndpoint } returns "https://fhirtst.mdanderson.org/fhirtst/oauth2/token"
                            every { privateKey } returns loadPrivateKey
                        }
                    every { ehrUserId } returns "1"
                }
        }

    private fun getTenants(): List<Tenant> {
        return listOf(apposndTenant, psjProdTenant, mdaProdTenant, mdaTstTenant)
    }

    fun getTenant(tenantMnemonic: String): Tenant {
        return getTenants().first { it.mnemonic == tenantMnemonic }
    }
}
