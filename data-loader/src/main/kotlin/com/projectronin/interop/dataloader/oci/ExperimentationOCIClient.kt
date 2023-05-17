package com.projectronin.interop.dataloader.oci

import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.io.FileInputStream

class ExperimentationOCIClient(
    tenancyOCID: String,
    userOCID: String,
    fingerPrint: String,
    privateKey: String,
    namespace: String,
    regionId: String,
    private val experimentationBucket: String = "prod-experimentation"
) : OCIClient(
    tenancyOCID = tenancyOCID,
    userOCID = userOCID,
    fingerPrint = fingerPrint,
    privateKey = privateKey,
    namespace = namespace,
    infxBucket = "",
    datalakeBucket = "",
    regionId = regionId
) {
    private val logger = KotlinLogging.logger { }

    companion object {
        fun fromEnvironmentVariables(): ExperimentationOCIClient {
            // Values can be found in Vault mirth-connector when setting up your environment
            val namespace: String = "idoll6i6jmjd"
            val tenancyOCID: String = "ocid1.tenancy.oc1..aaaaaaaapjtgtxtifoh4yi5wq3o5vkafdnd5nplvew4phqtrntp74pehz4yq"
            val userOCID: String = "ocid1.user.oc1..aaaaaaaaxubevuufpqnsj2oci7b2imo6z3unvovhba5o3whk7naz2cqfxcaa"
            val fingerPrint: String = "58:2f:24:49:1f:ae:f3:a6:3a:1f:9d:de:ee:49:a5:c2"
            val regionId: String = "us-phoenix-1"
            val privateKey: String = "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlKS1FJQkFBS0NBZ0VBdzJuN0oyYU9CLzNiaUlUazU1QkNuejNJUVI4enltNnBoUTNuaUdRZVhNYmU3cE10CjU2dlorVkphYmlHcWw0bVJiRGd4RWozQkxPeEUxSWxHMUx1RkcvaEhISWpUendUdTBZTjRuWHVwWFBoTk00cVIKNlBXUFh4MW1yZUplWXFwcHAvQkhxRnhBYTYwQzQwMTVLR2EvSkJRT1V1M2Z1Z2xnaGo4UkJ0QkZESFQ3VFZuagpsU2VkWXpVVnFtT1Z2TFNqVTBSemNkejVhd3ZJVTRPTFd2Mzd2QkZxajBMSnVLTmhtS041M09PQ2ZPQ1UrYm5QCjZHc20xamVIbzlVM3d1aG1qTXpOUmthYmh4dS9aK3h0cGlLQXk5RUs3SEJlclVHamsvWVR4bTA5eUxKcytwWHQKK0o2Y2E5aW1oWE4vNklubXhoNy84RVBwWndITFZXbEJVT21ISTRQaGNqeWdFRmIrTTM5VEp4b2RVZjA1RmE2Ugo0d2NaYk1KdDczdDNBMjhPZ0NJMjkrMU0yeFFnV203N0pXWndhVEc0N2xMbUpSMTErck5wTWFYL1hqL0Z3a3pmCmdacEVROW5Qb1ZGTWphNnd2N1h3VHBPeDU1OVR2UTVqZFlnSFdrN2EvWWl1ekdvdjJuQ0o2UEZPMXl4U0xGdnQKeVR2SERsaDZPV0dSQW5Fa1VEMVN5RGtoR0VjaVludEVLQlJBNVRydkNxWWpNSXF1b3MxYjVoWEdORXpjRnBzcApLQ1U1dmprMW1lNkE0aXU5WTFjSFh2OU5qcm5tQkxLWUJkWk5tOHRtNzQ3ckZMbmRIYmVKT05RN1RGbVNrbnJoClphQlViSlFHV2YzL2dsbGdRYWdQTUw5bStMdThaZFJZeFhLZDQ1QW1jbUYyOUhxSndJeVhBNUM1NlNFQ0F3RUEKQVFLQ0FnQjlDNGRrbWhLTytKb1BqUUt4VHpYaWtuVmZmcWowZzhYRVY3WnVKNnJxTVhRY2RGL3VuS0I2RTRHOApjWnlwSmFYQisxOGpUaE1jUFFCNmhIR24xV3NERCtmcFVZRkFPdWJiZU9hbzE2WHowUXdOekVFOE1rNlJUYzRUCnZRUXEzT09KVEk5K0k3SXhkTitoTmtVdVhqazN6QUd5ZlhjcG83QjVsMGU4OVc4SjNwMmt6TDErQjRDR0tFVDUKNkltNzNGZWJBL1ZmeWtoc2dIaXJRRVRmY0NFekltM3V5STB1c3BYVVI5L0dRbXVRbjJHVlA1clVIaVVXOVNlOQo1Y1d0Mm1zczlDenpLR0dEbm5UbitubmIwcjVhTGlWd1VTTzBlSWxGT0RWRlIwZys4alljVCs2Y3BscHdCSTNpClhadGU0VWVrUmRmL0ZtWDN2VVV4SFk1blNWaG9la0x4WnhTUzBlR0tpRGVZVWhrTDVPdEE3cnZYdWRLQXJERXoKMDAyVFI1aFJGbG9RRlRwSFgvQ0ZKbUJ6T094dElteHFPNkpRRFlsUVhxYkNKaWt2QWp6M1lXV1ZoMDBiNXVVcQpUdldMbGo1akQwdkdmUUhmUWVkWUo1OE14cGdiZks3cVBYNHpOTGRacnBGTWIrbEtKdG1kOFlRRnV6N0xYMmJPCm1CWFJQL2hyMVRsTGwrdDdGWDMxaXV2d3o4eWVIMFZ3bmVON0JFVFM4bm9Jc0FiNjlKeFVHdjBuYVpkSGpqTW8KcWluZGc3TysxaGh3MThsaGRlM1M0M0k5UVdKZ0lMdkhiNmhVdWVIOXRmT3I2dkJTRWlIM3lpOEJIR3E5M3lnSQpHNWg1WEI1Q3hIUFV4aExaOHozUms1eklKaFlyQk81NHVEYUtmblNJdFdqeXF4MmZWUUtDQVFFQTI2NDVGb1NoCmlnWWZnMWFNVUh5bnFOWlN0UEVTUGxlOXNUVk9KSlhXS08xZnlPaWhxaEtmNDFvSU1IdmFkZThsRVk2c2dLNCsKelN2V2tpdkNKa3VkcjU2MjFSZm9UeW51R1Y4bE1EODUzcjZJUlJLWXBFZ3Q2bVd4eGxtT2Jtb0VaOXpLUXE0VgpSNzFmRk9CaHVobC82M1cyWjdGUGoybWpVTlY2bUVwRTlxc1ZYR0tIT2V5b0pBV1dEU3BuZEVtdXV2VXZEWUNHClZhSHpBMytTKzljVEQzZUNyTTVZelBwWXU2REgyWUZXKzhxLzBDdm9WaWtNRndJRWlIUE85NFg2SVNTMnVBTlQKbHloUXFDV21UTGpQS3pwemFRa3RwWitKTlJuc3RZTXk3QVBSMVNjU2VLYytvOGo0ZldGRVQwRTQwczdCT0w5QgozK1NwZEkwMEtzUldxd0tDQVFFQTQ3aXlxQnpUUWdaVVRUeEd1MkpCQ283dkZlWWxmemN0b0IwWDIvV0U3UWc0CjdWcmZhNlIzNm13VVZ4QlN5WlhoVzRJMkdiV3V3Sm9sV2RiU1p4bHpUQlVqQjFTeVBKT2RwOFZvY1JIQmYxSEkKL1dvbVV4VldMcHgyN3IrYWpabExRRjF0NTFiWnA4bkhSRWJDVVM4cHJWWnlOaFFVdkVtKzlzNTRCTVg2WlQwRwpCMnQ0NHkzWjgyNUpNVUxoSHNIbldFdEVSYnNrNGZhdlk5Qkhsb24rQ2N2N1g4d1dOTUl4eFd6Nll5cjQ2cHM5CkpxVHJ6d3NlNEZxaWNzUEJKOGRHT2dlcTZtMjJpWHRJSXlQMEEwWmQ0eHQ1aElFMGI2cnVIWHFhVEpvOWlhYXkKallDUDJ2VzJ5KzhzWjVZK25QMTM0SS9aTmovYUZNc3NyZXZ2VEtrdll3S0NBUUVBa0lXV3d3eXdaV2hQMVF6YwpOSnM5aGFLeU9KMmNIZTJGT3c3UmdOb2VVVUNRMGxEUkFsMU1XTHJEUm5wRXAyK1QySEFmWm15N25pam4rYlBWCi8zR2hwSWJxeTFmMkdoYUFzS3BhZ2RvbWpUYVYyYk01UG1MSjZqbks4YTI3T1VKLzhZQmNnKytZeW1CNEsvak4KOGl1U2IybzZmUTgyUXlnQkxFMjFZSGtEMDYrTDF3SnU2aG1hS0ZDUWZjclcwcmNpdjVaTHhUczNwU0J0Q25FawpWcEFRMXcwTnAvZjhqYVZwT002Mmk0NCtsbG0xUlJPTnZ4a3ovZkx5M240cHU4ZWxYYWFVbW94OXNxVHdJeDdaCndBR1pNVnc3enU4ZDI2Y1FFaUlqQ3Vyc1ZqN1JEY1N0cnpoL3lpcU41NVpVNVRzSVVnOTVLSWNTcEdQL1kxRDkKNytESkR3S0NBUUJEamgxaUJmc2VFU2FkS3c2SXRHTTdNSi80elNlK2o2MzNXK29pRkxuMFg0WVQwK3VIMFdqSgpvSHFBUVpWdHZBcjR6SzRqL21Qek8wZHRXVzlkcW8wcnV2R0RVVHIzQVl1NW83U1NKb1JTc3Y1L1B5aFRhWGNCCnFPVVhlcnZzMmRjYkp4eGRqc1JQSkdyNjlFcmRMcktjZTVLeVBFN1NkTVkwRFpoSlFqcEFJbFJSaXk1M3ByaUwKb09JUnZSZHZ1bkpaSFNzSnpMVHFXL1k0RkNmSW52TkFFTVZuNi9HQ2dVR21vZWU5MTVlMGNZMDRrNkJvOFpHZApXUVhzS2VwNVZJS2U0SHR4eEU1c2hxZ2wxSnFsQmhFQU9MeFo5TUtMaGhmUm92ZDNWaUovWFhhR3grYmxQQk9ZCmVFR2lFVTVONEZ2WndKTGRweCtaSDVVSG00Q0lLNENsQW9JQkFRQ1J4ZFg5MVA2dGp4alN6c2NSUHU3US8yQi8KNjVSSEZyRnZJSHhVbyt6OGxiNWJzU0tEb2xNOEwwVldDbERBWXFVaE9PbXprRVdXQ3g3RGU0allqYzdSUFRLMwp1UGlCaGVGU080L3U1a0gyODkxNmY1KzBMbm1maWVrUGo4bVRVRTdVb0ZnNlFqNkNRNGZ3a01SOFpJUUE3KzN1CnphM0tvK3Q2WDlHNDlLaDVzV2dXUjlqZDh6WVNhRE42QW14Z2lvdTVxZDNWL2hKZ0R6dnYzN2hzdHJsN1hYLzcKYm4vNTcyVldlK3gwbUZ2R2Z3aWExdVYxK0t1WitsRW5lcis0Y0lEY1JjUStSQm00ZGtmOHlZU3VmM1RZOFNQNQpyRGhLNzBpVmZQVHZvZ0N5M3ZkSTMvRVU1WTdpMFU3Wk16V3J5VElpSmcwWHZvL3NtWm5RekFxK1czYk0KLS0tLS1FTkQgUlNBIFBSSVZBVEUgS0VZLS0tLS0K"
            return ExperimentationOCIClient(
                tenancyOCID = tenancyOCID,
                userOCID = userOCID,
                fingerPrint = fingerPrint,
                privateKey = privateKey,
                namespace = namespace,
                regionId = regionId
            )
        }
    }

    /**
     * Uploads the file named in [fileName] to the experimentation OCI bucket under the given [tenant]
     * and [resourceType].  [resourceType] will usually be a FHIR resource type, but doesn't have to be.  DP is usually
     * expecting the file to be JSON, but that could change depending on the specific file and what it's
     * being used for.
     *
     * Multiple files belonging to the same extract should be uploaded with the same timestamp, as that is what will
     * be used to group them by DP.
     */
    fun uploadExport(tenant: Tenant, resourceType: String, fileName: String, timeStamp: String): Boolean {
        val fileNameOut = fileName.substringAfterLast('/', fileName)
        val fileNameToUpload =
            "${tenant.name.lowercase()}_data_exploration/${resourceType.lowercase()}/$timeStamp/$fileNameOut".replace(":", "-")

        // If you upload enough files in a run, OCI will eventually fail on a few.  Just log it and move on
        // to the next file.
        return runCatching {
            upload(experimentationBucket, fileNameToUpload, FileInputStream(fileName))
        }.getOrElse {
            logger.error { "Error uploading $fileName to OCI: ${it.message}" }
            false
        }
    }
}
