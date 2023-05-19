package com.projectronin.interop.dataloader.cerner.service.authentication

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.http.request
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.auth.AuthenticationService
import com.projectronin.interop.ehr.cerner.auth.CernerAuthentication
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.Base64

// This is a copy of the regular cernerAuthenticationService, but it has no references to spring

class CernerAuthenticationService(private val client: HttpClient) :
    AuthenticationService {
    private val logger = KotlinLogging.logger { }
    override val vendorType = VendorType.CERNER

    private val scope: String by lazy {
        // add as needed
        val resources = listOf(
            "Patient",
            "Appointment",
            "Location",
            "Observation",
            "Condition"
        )

        val scopes = mutableListOf<String>()
        resources.map {
            scopes.add("system/$it.read")
        }
        logger.debug { "Cerner auth scope: $scopes" }
        scopes.joinToString(separator = " ")
    }

    override fun getAuthentication(tenant: Tenant): Authentication {
        val vendor = tenant.vendorAs<Cerner>()
        val authURL = vendor.authenticationConfig.authEndpoint
        val clientIdWithSecret = "${vendor.authenticationConfig.accountId}:${vendor.authenticationConfig.secret}"
        val encodedSecret = Base64.getEncoder().encodeToString(clientIdWithSecret.toByteArray())

        val httpResponse = runBlocking {
            client.request("Cerner Auth for ${tenant.name}", authURL) { authURL ->
                post(authURL) {
                    headers {
                        append(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                        append(HttpHeaders.Authorization, "Basic $encodedSecret")
                    }
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("grant_type", "client_credentials")
                                append("scope", scope)
                            }
                        )
                    )
                }
            }
        }
        val response = runBlocking { httpResponse.body<CernerAuthentication>() }

        logger.debug { "Completed authentication for $authURL" }
        return response
    }
}
