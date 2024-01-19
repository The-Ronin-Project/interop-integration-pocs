package com.projectronin.interop.dataloader.base

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.dataloader.oci.ExperimentationOCIClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.mockk
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths
import kotlin.io.path.createDirectory

/**
 * Base class used to avoid rewriting the same functions in every loader.
 */
abstract class BaseLoader() {
    protected val ehrDataAuthorityClient = mockk<EHRDataAuthorityClient>(relaxed = true)
    protected val datalakePublishService = mockk<DatalakePublishService>(relaxed = true)

    val expOCIClient = ExperimentationOCIClient.fromEnvironmentVariables()
    val logger = KotlinLogging.logger { }
    val httpClient = HttpSpringConfig().getHttpClient()

    init {
        runCatching { Paths.get("loaded").createDirectory() }
    }

    /**
     * Writes the list of resources out to a file named [fileName].  More often than not, [resources] will be a list
     * of FHIR resources, but they don't have to be.
     */
    @Deprecated("just use writeAndUploadResources")
    fun writeFile(
        fileName: String,
        resources: List<Any>,
    ) {
        if (resources.isNotEmpty()) {
            BufferedWriter(FileWriter(File(fileName))).use { writer ->
                writer.write(
                    JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resources),
                )
                writer.close()
            }
        }
    }

    /**
     * Uploads [fileName] to OCI under the [resourceType] and [timeStamp] folders.  Returns true if successful.
     */
    @Deprecated("just use writeAndUploadResources")
    fun uploadFile(
        fileName: String,
        tenant: Tenant,
        resourceType: String,
        timeStamp: String,
    ) = expOCIClient.uploadExport(tenant, resourceType, fileName, timeStamp)

    fun <T : Resource<*>> writeAndUploadResources(
        tenant: Tenant,
        fileName: String,
        resources: List<T>,
        timeStamp: String,
        dryRun: Boolean = true,
    ) {
        if (resources.isEmpty()) return
        val resourceType = resources.first().resourceType.lowercase()

        val fileDirectory = "loaded/$resourceType"
        val pathName = "$fileDirectory/$fileName.json"
        runCatching { Paths.get(fileDirectory).createDirectory() }

        // logger.info { "Writing $pathName" }
        BufferedWriter(FileWriter(File(pathName))).use { writer ->
            writer.write(
                JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resources),
            )
            writer.close()
        }
        if (!dryRun) {
            // logger.info { "Uploading $pathName to OCI" }
            expOCIClient.uploadExport(tenant, resourceType, pathName, timeStamp)
        } else {
            logger.info { "Not uploading $pathName to OCI, mark dryRun = false to upload" }
        }
    }

    fun uploadString(
        tenant: Tenant,
        fileName: String,
        data: String,
        timeStamp: String,
    ) {
    }

    abstract fun main()

    // Often times we're getting MRNs, this is a way to
    protected fun getMRNs(): Set<String> = this.javaClass.getResource("/mrns.txt")!!.readText().split("\r\n", "\n").toSet()

    protected abstract fun getPatientsForMRNs(mrns: Set<String> = getMRNs()): Map<String, Patient>
}
