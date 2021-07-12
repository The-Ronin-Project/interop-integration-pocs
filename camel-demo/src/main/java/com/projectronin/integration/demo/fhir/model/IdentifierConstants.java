package com.projectronin.integration.demo.fhir.model;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public class IdentifierConstants {
	private static final String IDENTIFIER_TYPE_CODE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";

	public static final CodeableConcept MRN = new CodeableConcept().setText("Medical Record Number").setCoding(asList(
			new Coding().setCode("MR").setDisplay("Medical Record Number").setSystem(IDENTIFIER_TYPE_CODE_SYSTEM)));

	public static final CodeableConcept SSN = new CodeableConcept().setText("Social Security Number").setCoding(asList(
			new Coding().setCode("SS").setDisplay("Social Security Number").setSystem(IDENTIFIER_TYPE_CODE_SYSTEM)));

	public static final String SS_SYSTEM = "http://hl7.org/fhir/sid/us-ssn";

	public static final CodeableConcept DRIVERS_LICENSE = new CodeableConcept().setText("Driver's License").setCoding(
			asList(new Coding().setCode("DL").setDisplay("Driver's License").setSystem(IDENTIFIER_TYPE_CODE_SYSTEM)));

	public static final CodeableConcept PASSPORT = new CodeableConcept().setText("Passport Number").setCoding(
			asList(new Coding().setCode("PPN").setDisplay("Passport Number").setSystem(IDENTIFIER_TYPE_CODE_SYSTEM)));

	public static final CodeableConcept ACCOUNT_NUMBER = new CodeableConcept().setText("Account Number").setCoding(
			asList(new Coding().setCode("AN").setDisplay("Account Number").setSystem(IDENTIFIER_TYPE_CODE_SYSTEM)));

	public static final CodeableConcept PERSON_NUMBER = new CodeableConcept().setText("Person Number").setCoding(
			asList(new Coding().setCode("PN").setDisplay("Person Number").setSystem(IDENTIFIER_TYPE_CODE_SYSTEM)));

	private static final Map<String, CodeableConcept> CONCEPT_BY_CODE;
	private static final Map<String, String> SYSTEM_BY_CODE;

	static {
		CONCEPT_BY_CODE = new HashMap<>();
		CONCEPT_BY_CODE.put("MR", MRN);
		CONCEPT_BY_CODE.put("SS", SSN);
		CONCEPT_BY_CODE.put("DL", DRIVERS_LICENSE);
		CONCEPT_BY_CODE.put("PPN", PASSPORT);
		CONCEPT_BY_CODE.put("AN", ACCOUNT_NUMBER);
		CONCEPT_BY_CODE.put("PN", PERSON_NUMBER);

		SYSTEM_BY_CODE = new HashMap<>();
		SYSTEM_BY_CODE.put("SS", SS_SYSTEM);
	}

	public static Optional<CodeableConcept> getCodeableConcept(String code) {
		return Optional.ofNullable(CONCEPT_BY_CODE.get(code));
	}

	public static Optional<String> getSystem(String code) {
		return Optional.ofNullable(SYSTEM_BY_CODE.get(code));
	}
}
