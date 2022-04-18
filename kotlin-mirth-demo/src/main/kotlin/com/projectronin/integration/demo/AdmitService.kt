package com.projectronin.integration.demo

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.GenericSegment
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Varies
import ca.uhn.hl7v2.model.v281.datatype.CX
import ca.uhn.hl7v2.model.v281.datatype.XPN
import ca.uhn.hl7v2.model.v281.message.ADT_A01
import ca.uhn.hl7v2.model.v281.segment.MSH
import ca.uhn.hl7v2.model.v281.segment.PID
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient

class AdmitService {
    private val identifiers = Identifiers()

    fun process(hl7Message: String): String {
        println("Parsing new message")

        val hapiContext = DefaultHapiContext(ValidationContextFactory.noValidation())
        hapiContext.modelClassFactory = CanonicalModelClassFactory("2.8.1")

        val hl7Parser = hapiContext.pipeParser
        val message = hl7Parser.parse(hl7Message)

        val tenant = getTenant(message)

        tenant?.let {
            if (message is ADT_A01) {
                return process(message, tenant)
            }
        }

        return ""
    }

    private fun getTenant(message: Message): Int? {
        val msh = message.get("MSH")
        val hl7FacilityId = if (msh is MSH) {
            msh.sendingFacility.namespaceID.value
        } else {
            val messageType = (msh as GenericSegment).getField(4)[0] as Varies
            Terser.getPrimitive(messageType.data, 1, 1).value
        }

        return when (hl7FacilityId) {
            "1" -> 1001
            "MDA" -> 1002
            "PSJ" -> 1003
            else -> null
        }
    }

    private fun process(input: ADT_A01, tenant: Int): String {
        val pid = input.pid
        val patient = createPatient(pid, tenant)

        val jsonParser = FhirContext.forR4().newJsonParser()
        val jsonPatient = jsonParser.encodeResourceToString(patient)
        println("JSON Patient = $jsonPatient")
        return jsonPatient
    }

    private fun createPatient(pid: PID, tenant: Int): Patient {
        val patient = Patient()
        with(patient) {
            identifier = createIdentifiers(pid, tenant)
            name = createNames(pid.patientName)
            active = true
        }
        return patient
    }

    private fun createIdentifiers(pid: PID, tenant: Int): List<Identifier> {
        val identifiers = mutableListOf<Identifier>()

        for (patientIdentifier in pid.patientIdentifierList) {
            createIdentifier(patientIdentifier)?.let { identifiers.add(it) }
        }

        createIdentifier(pid.patientAccountNumber)?.let { identifiers.add(it) }

        val tenantIdentifier = Identifier().setSystem("http://projectronin.com/fhir/tenant").setValue(tenant.toString())
        identifiers.add(tenantIdentifier)

        return identifiers
    }

    private fun createIdentifier(hl7Identifier: CX): Identifier? {
        val identifierTypeCode = hl7Identifier.identifierTypeCode.value

        val type = identifiers.getCodeableConcept(identifierTypeCode)
        type?.let {
            val id = hl7Identifier.idNumber.value
            val system = identifiers.getSystem(identifierTypeCode)

            return Identifier().setSystem(system).setValue(id).setType(it)
        }

        return null
    }

    private fun createNames(hl7Names: Array<XPN>): List<HumanName> {
        val names = mutableListOf<HumanName>()
        for (hl7Name in hl7Names) {
            val fhirName = HumanName()
            with(fhirName) {
                family = hl7Name.familyName.surname.value
                addGiven(hl7Name.givenName.value)
                addGiven(hl7Name.secondAndFurtherGivenNamesOrInitialsThereof.value)
                addPrefix(hl7Name.prefixEgDR.value)
                addSuffix(hl7Name.suffixEgJRorIII.value)
                addSuffix(hl7Name.professionalSuffix.value)
            }
            names.add(fhirName)
        }
        return names
    }
}
