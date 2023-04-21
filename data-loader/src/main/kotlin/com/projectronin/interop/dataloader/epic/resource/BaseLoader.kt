package com.projectronin.interop.dataloader.epic.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.dataloader.epic.ExperimentationOCIClient
import com.projectronin.interop.tenant.config.model.Tenant
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Base class used to avoid rewriting the same functions in every loader.
 */
abstract class BaseLoader(private val expOCIClient: ExperimentationOCIClient) {
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
}
