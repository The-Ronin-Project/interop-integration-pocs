package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class BinaryService(
    private val epicClient: EpicClient,
    private val authenticationBroker: EHRAuthenticationBroker,
    private val client: HttpClient
) {
    private val logger = KotlinLogging.logger { }

    fun getBinaryData(tenant: Tenant, binaryFhirId: String): String {
        return runBlocking { epicClient.get(tenant, "/api/FHIR/R4/Binary/$binaryFhirId", emptyMap()).bodyAsText() }
    }

    fun getBinary(tenant: Tenant, binaryFhirId: String): Binary? {
        return get(tenant, "/api/FHIR/R4/Binary/$binaryFhirId", emptyMap())
    }

    /**
     * Stolen from EpicClient and adjusted to use the FHIR JSON ContentType.
     */
    fun get(tenant: Tenant, urlPart: String, parameters: Map<String, Any?> = mapOf()): Binary? {
        // Authenticate
        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")

        val requestUrl =
            if (urlPart.first() == '/') {
                tenant.vendor.serviceEndpoint + urlPart
            } else {
                urlPart
            }

        val resource: Resource<*> = runBlocking {
            val response: HttpResponse = client.get(requestUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.FhirJson)
                parameters.map {
                    val key = it.key
                    val value = it.value
                    if (value is List<*>) {
                        value.forEach { repetition ->
                            parameter(key, repetition)
                        }
                    } else {
                        value?.let { parameter(key, value) }
                    }
                }
            }

            response.body()
        }

        return if (resource is Binary) {
            resource
        } else {
            logger.warn { "Could not load Binary $urlPart: $resource" }
            null
        }
    }
}
