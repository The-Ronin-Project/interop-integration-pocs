package com.projectronin.interop.dataloader.base

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.oci.ExperimentationOCIClient
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
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
    fun writeFile(fileName: String, resources: List<Any>) {
        if (resources.isNotEmpty()) {
            BufferedWriter(FileWriter(File(fileName))).use { writer ->
                writer.write(
                    JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resources)
                )
                writer.close()
            }
        }
    }

    /**
     * Uploads [fileName] to OCI under the [resourceType] and [timeStamp] folders.  Returns true if successful.
     */
    fun uploadFile(fileName: String, tenant: Tenant, resourceType: String, timeStamp: String) =
        expOCIClient.uploadExport(tenant, resourceType, fileName, timeStamp)

    abstract fun main()

    // Often times we're getting MRNs, this is a way to
    protected fun getMRNs(): Set<String> =
        this.javaClass.getResource("/mrns.txt")!!.readText().split("\r\n", "\n").toSet()

    protected abstract fun getPatientsForMRNs(mrns: Set<String>): Map<String, Patient>
}
