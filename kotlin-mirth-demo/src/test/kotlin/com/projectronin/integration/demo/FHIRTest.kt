package com.projectronin.integration.demo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import org.junit.jupiter.api.Test

class FHIRTest {
    @Test
    fun process() {
        val text = this::class.java.getResource("/output.json")!!.readText()
        val response = JacksonManager.objectMapper.readValue<Response>(text)

        response.data?.practitionerList?.forEach { p ->
            val identifier =
                p.identifier.find { it.system?.value == "urn:oid:1.2.840.114350.1.13.297.3.7.2.697780" && it.type?.text == "EXTERNAL" }
            println("${p.name[0].family}\t${p.name[0].text}\t${identifier?.value}\t${p.id.value}")
        }
    }
}

data class Response(val data: PractitionerList?)
data class PractitionerList(@JsonProperty("PractitionerList") val practitionerList: List<PartialPractitioner>?)
data class PartialPractitioner(val identifier: List<Identifier>, val id: Id, val name: List<HumanName>)
