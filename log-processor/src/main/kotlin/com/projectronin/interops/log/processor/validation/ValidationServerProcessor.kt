package com.projectronin.interops.log.processor.validation

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.validation.client.IssueClient
import com.projectronin.interop.validation.client.ResourceClient
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import com.projectronin.interop.validation.client.generated.models.Severity
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

fun main() {
    ValidationServerProcessor().process()
}

class ValidationServerProcessor {
    private val hostUrl = System.getenv("VALIDATION_URL")
    private val authUrl = System.getenv("VALIDATION_AUTH_URL")
    private val audience = System.getenv("VALIDATION_AUDIENCE")
    private val clientId = System.getenv("VALIDATION_AUTH_CLIENT")
    private val secret = System.getenv("VALIDATION_AUTH_SECRET")

    private val httpClient = HttpSpringConfig().getHttpClient()
    private val authService = ValidationAuthenticationService(httpClient, authUrl, audience, clientId, secret, true)
    private val resourceClient = ResourceClient(hostUrl, httpClient, authService)
    private val issueClient = IssueClient(hostUrl, httpClient, authService)

    private val tenant = "v7r1eczk"
    private val after =
        OffsetDateTime.of(2023, 1, 23, 20, 0, 0, 0, ZoneId.of("America/Chicago").rules.getOffset(Instant.now()))

    fun process() {
        var lastSeen: UUID? = null
        val errors = mutableMapOf<Error, Int>()
        while (true) {
            val resources = runBlocking {
                resourceClient.getResources(
                    listOf(ResourceStatus.REPORTED),
                    Order.DESC,
                    25,
                    lastSeen,
                    organizationId = tenant
                )
            }
            if (resources.isEmpty()) break

            resources.filter { it.severity == Severity.FAILED && it.createDtTm >= after }
                .forEach { resource ->
                    val issues = runBlocking { issueClient.getResourceIssues(resource.id, Order.ASC) }
                    issues.filter { it.severity == Severity.FAILED }.forEach { issue ->
                        val error = Error(resource.resourceType, issue.location, issue.type)
                        val count = errors[error] ?: 0
                        errors[error] = count + 1
                    }
                }

            if (resources.last().createDtTm < after) break

            lastSeen = resources.last().id
        }

        errors.forEach {
            println("${it.value}: ${it.key}")
        }
    }

    data class Error(
        val resourceType: String,
        val location: String,
        val code: String
    )
}
