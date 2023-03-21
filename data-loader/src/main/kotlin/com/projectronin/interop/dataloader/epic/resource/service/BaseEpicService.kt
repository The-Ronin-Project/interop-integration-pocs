package com.projectronin.interop.dataloader.epic.resource.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking

/**
 * Slightly modified form of the EpicFHIRService
 */
abstract class BaseEpicService<T : Resource<T>>(val epicClient: EpicClient) {
    abstract val fhirResourceType: Class<T>
    abstract val fhirURLSearchPart: String
    private val standardParameters: Map<String, Any> = mapOf("_count" to 50)

    fun getResourceListFromSearch(tenant: Tenant, parameters: Map<String, Any?>): List<T> {
        return getBundleWithPaging(tenant, parameters).entry.mapNotNull { it.resource }
            .filterIsInstance(fhirResourceType)
    }

    fun getResourceListFromSearchSTU3(tenant: Tenant, parameters: Map<String, Any?>): List<T> {
        return getBundleWithPagingSTU3(tenant, parameters).entry.mapNotNull { it.resource }
            .filterIsInstance(fhirResourceType)
    }

    fun getBundleWithPaging(
        tenant: Tenant,
        parameters: Map<String, Any?>
    ): Bundle {
        val standardizedParameters = standardizeParameters(parameters)

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        epicClient.get(tenant, fhirURLSearchPart, standardizedParameters)
                    } else {
                        epicClient.get(tenant, nextURL!!)
                    }
                httpResponse.body<Bundle>()
            }

            responses.add(bundle)
            nextURL = bundle.link.firstOrNull { it.relation?.value == "next" }?.url?.value
        } while (nextURL != null)
        return mergeResponses(responses)
    }

    fun getBundleWithPagingSTU3(
        tenant: Tenant,
        parameters: Map<String, Any?>
    ): Bundle {
        val standardizedParameters = standardizeParameters(parameters)

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        epicClient.get(tenant, fhirURLSearchPart, standardizedParameters)
                    } else {
                        epicClient.get(tenant, nextURL!!)
                    }
                httpResponse.body<STU3Bundle>()
            }

            responses.add(bundle.transformToR4())
            nextURL = bundle.link.firstOrNull { it.relation?.value == "next" }?.url?.value
        } while (nextURL != null)
        return mergeResponses(responses)
    }

    fun mergeResponses(
        responses: List<Bundle>
    ): Bundle {
        var bundle = responses.first()
        responses.subList(1, responses.size).forEach { bundle = mergeBundles(bundle, it) }
        return bundle
    }

    fun standardizeParameters(parameters: Map<String, Any?>): Map<String, Any?> {
        val parametersToAdd = standardParameters.mapNotNull {
            if (parameters.containsKey(it.key)) {
                null
            } else {
                it.toPair()
            }
        }
        return parameters + parametersToAdd
    }

    fun searchByID(tenant: Tenant, resourceFHIRId: String): T {
        return runBlocking {
            epicClient.get(tenant, "$fhirURLSearchPart?_id=$resourceFHIRId")
                .body(TypeInfo(fhirResourceType.kotlin, fhirResourceType))
        }
    }
}
