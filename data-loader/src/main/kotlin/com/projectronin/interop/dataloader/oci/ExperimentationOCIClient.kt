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
            val namespace: String = System.getenv("LOAD_OCI_NAMESPACE")
            val tenancyOCID: String = System.getenv("LOAD_OCI_TENANCY_OCID")
            val userOCID: String = System.getenv("LOAD_OCI_USER_OCID")
            val fingerPrint: String = System.getenv("LOAD_OCI_FINGERPRINT")
            val regionId: String = System.getenv("LOAD_OCI_REGION_ID")
            val privateKey: String = System.getenv("LOAD_OCI_PRIVATE_KEY")
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
