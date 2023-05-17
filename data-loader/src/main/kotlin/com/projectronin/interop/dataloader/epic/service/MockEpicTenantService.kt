package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk

// some day this should be pulling from the actual db / tenant server, but let's abstract this a bit
class MockEpicTenantService {
    // Values can be found in the customer you want to pull froms tenant config when setting up your environment
    // val mrnSystem: String = System.getenv("LOAD_MRN_SYSTEM")
    // val loadClientId: String = System.getenv("LOAD_CLIENT_ID")
    // val loadServiceEndpoint: String = System.getenv("LOAD_SERVICE_ENDPOINT")
    // val loadAuthEndpoint: String = System.getenv("LOAD_AUTH_ENDPOINT")
    // val loadPrivateKey: String = System.getenv("LOAD_PRIVATE_KEY")

    val mrnSystem: String = "urn:oid:1.2.840.114350.1.13.412.2.7.5.737384.14"
    val loadClientId: String = "93bca0b8-985e-41d9-9fe2-3024e2c0a967"
    val loadServiceEndpoint: String = "https://fhir.mdanderson.org/FHIR"
    val loadAuthEndpoint: String = "https://fhir.mdanderson.org/FHIR/oauth2/token"
    val loadPrivateKey: String = "-----BEGIN PRIVATE KEY-----MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDbZdCF1V8cMWKjrq7Igs8RiztEhyyBb/1sM4pghHrcjkxdE+RY8gz/Ge2Gcle6EMgNOiGSeHRqvmcJytyB/aifbwhueGBhpGKJN21JMGP1t9VOeI89CyiA2B5yiDWnYipuRS81E4GiMHCOfaN7RULBlrU5riWGYxo7QGSy2ycnw94n/UuUHLgIO8tPRcvjgToZIkBlnBPLmUw4xjHBS3S1Qj7g9LaNw90IurnK7RQ7JGLqD5qEjn9UQpy/kzR4sKI3rf5OZZ+ShtJBEPHBqKLD0a/j8W0Q7B8HyVIGXGF0Ht4F0UMQKgajOv78GvQgvtubq+sFrhFbV+mfCBxQoYu5AgMBAAECggEBAMJLGi1LVl0rRwVZjyO25BXPTX9Ujg9a0/zcb/EloYBs7CeJlDFIukW3+meUHyiB8Af2sQB7d+2IDMvZQQpFTu1ScQ8Tv5b48uI9maS1G/DhCh/iGEeg+A+SVPRst8cx4g2hazr/uU+ewi8OCJcEMSgrhPY9yLGIVhSe5A6YkcZdxPOL3vH8txoj+HQJ4GJcGGA5wQosFOf+y8ZmXDK9VWnqAXemNDFaDWlePFxO6Uq1VY7JzZBw/VA3clXnKO0lBMXobMxKS23pfo+YmdYQK0BiyZhZl9APsuM3s8DpIXHWHXprStxbm/LEcDlzn39yNQ7313fq9jau3iOezQCGwOECgYEA9hM7twSDbfxIUIG+/3OmCllTYBy+daUp+8Urakv4gzMNT0sjmXsrzqJvn6X6anEIRWhDvLfz6VqBej8N7MyOKY0lypNqqQ6zpQ1i4RcKiQzj9moJJflmy76gKIoyx6930N102gMGkZnprJdCPUC6Asnj5x9iazE1H+hFIMz9fR0CgYEA5D8h68znLBNCd7BOqd8/PX4BdGu09k5dFuSRNrJI5Qm0FlmJ9SNl/3nDwBbEAagHRug/Lvn2kCXcMFZmEtZWopMPjUuHlXKcB4TlriaC0U/B5dqLhARM3JES1Gwlo1psG2uP5cjNfwMfgDA75MvzXCK8LKhLQGOUJ0IZEl2cck0CgYEAxaZtMHPPnWgmLYQJheW+WFGnRmvQD266ah/U32xmD/vRlP8leQIWDcMrJXCL0zM3EYjN36dhou2vUiGTbcBf78APusbtxRyp7sjxwxpeu/Y+pI5HCtloV/6lZeqbHwkxk/JNCU+k1w5pQte7vLdgsRy4kcwDoVXE9fv6in4cnYECgYBSyZ9pIvI6p2W1egqeTO5oRHsMmkCSEkxrkE4nk7Ui9kkOzj1e4A8QAj+BPgCsRSEWlAjO5jWLhV//XGc7r+jOoR7D9kBgcaishYS1jRxoBVDkKUfvg4vJeQlmaS+ht5t8uqAADbyTtDNJ0LT4wQ0tPyAX5pcc7MGBF9V2NuQXBQKBgEFhESmAzanSWEKd3tl8pFCQQsRjGWy5Wqpwd1JU+CUiNEO/usJbHpcvfpcuWEzuAVMMUBqvPIRI1PwPdmsPPzlsk5gwuLnMhHS11bDV6kFCcrKC1XW5oWesC4TwM2sZ9q//lxnzK8MNKYKooeXry/eIyInLSvgkLVaqNoBFZ1Kr-----END PRIVATE KEY-----"

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

    private val mdaProdTenant = mockk<Tenant> {
        every { mnemonic } returns "mdaoc"
        every { name } returns "mda"
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
        return listOf(tenant, psjProdTenant, mdaProdTenant)
    }

    fun getTenant(tenantMnemonic: String): Tenant {
        return getTenants().first { it.mnemonic == tenantMnemonic }
    }
}
