package com.projectronin.integration.demo

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding

class Identifiers {
    private val identifierTypeCodeSystem = "http://terminology.hl7.org/CodeSystem/v2-0203"

    private val mrn = CodeableConcept().setText("Medical Record Number").setCoding(
        listOf(
            Coding().setCode("MR").setDisplay("Medical Record Number").setSystem(identifierTypeCodeSystem)
        )
    )

    private val ssn = CodeableConcept().setText("Social Security Number").setCoding(
        listOf(
            Coding().setCode("SS").setDisplay("Social Security Number").setSystem(identifierTypeCodeSystem)
        )
    )

    private val driversLicense = CodeableConcept().setText("Driver's License").setCoding(
        listOf(Coding().setCode("DL").setDisplay("Driver's License").setSystem(identifierTypeCodeSystem))
    )

    private val passport = CodeableConcept().setText("Passport Number").setCoding(
        listOf(Coding().setCode("PPN").setDisplay("Passport Number").setSystem(identifierTypeCodeSystem))
    )

    private val accountNumber = CodeableConcept().setText("Account Number").setCoding(
        listOf(Coding().setCode("AN").setDisplay("Account Number").setSystem(identifierTypeCodeSystem))
    )

    private val personNumber = CodeableConcept().setText("Person Number").setCoding(
        listOf(Coding().setCode("PN").setDisplay("Person Number").setSystem(identifierTypeCodeSystem))
    )

    fun getCodeableConcept(typeCode: String): CodeableConcept? {
        return when (typeCode) {
            "MR" -> mrn
            "SS" -> ssn
            "DL" -> driversLicense
            "PPN" -> passport
            "AN" -> accountNumber
            "PN" -> personNumber
            else -> null
        }
    }

    fun getSystem(systemCode: String): String? {
        return when (systemCode) {
            "SS" -> "http://hl7.org/fhir/sid/us-ssn"
            else -> null
        }
    }
}
