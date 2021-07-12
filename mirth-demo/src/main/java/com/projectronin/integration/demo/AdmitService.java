package com.projectronin.integration.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.projectronin.integration.demo.fhir.model.IdentifierConstants;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v281.datatype.CWE;
import ca.uhn.hl7v2.model.v281.datatype.CX;
import ca.uhn.hl7v2.model.v281.datatype.DTM;
import ca.uhn.hl7v2.model.v281.datatype.ID;
import ca.uhn.hl7v2.model.v281.datatype.XAD;
import ca.uhn.hl7v2.model.v281.datatype.XPN;
import ca.uhn.hl7v2.model.v281.message.ADT_A01;
import ca.uhn.hl7v2.model.v281.segment.PID;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

/**
 * Service for handling admit messages.
 * 
 * @author Josh Smith
 */
public class AdmitService {

	public String process(String hl7Message) throws HL7Exception, IOException {
		System.out.println("Parsing new message");

		ResourceBundle.getBundle("ca.uhn.fhir.i18n.hapi-messages");

		HapiContext hapiContext = null;
		try {
			hapiContext = new DefaultHapiContext(ValidationContextFactory.noValidation());
			hapiContext.setModelClassFactory(new CanonicalModelClassFactory("2.8.1"));
			PipeParser parser = hapiContext.getPipeParser();

			Message message = parser.parse(hl7Message);

			TenantExtractor tenantExtractor = new TenantExtractor();
			Optional<Integer> tenantId = tenantExtractor.getTenantId(message);
			if (tenantId.isPresent()) {
				return process((ADT_A01) message, tenantId.get());
			}

			throw new RuntimeException("Unknown tenant");
		} finally {
			if (hapiContext != null) {
				hapiContext.close();
			}
		}
	}

	public String process(ADT_A01 input, int tenantId) throws HL7Exception, IOException {
		PID pid = input.getPID();

		Patient patient = createPatient(pid, tenantId);

		IParser jsonParser = FhirContext.forR4().newJsonParser();
		String jsonPatient = jsonParser.encodeResourceToString(patient);
		System.out.println("JSON Patient = " + jsonPatient);

		return jsonPatient;
	}

	private Patient createPatient(PID pid, int tenantId) throws HL7Exception, JsonProcessingException {
		Patient patient = new Patient();
		patient.setIdentifier(createIdentifiers(pid, tenantId));
		patient.setName(createNames(pid.getPatientName()));
		patient.setActive(true);

		patient.setAddress(createAddresses(pid.getPatientAddress()));
		patient.setBirthDate(createDate(pid.getDateTimeOfBirth()));
		patient.setCommunication(createCommunication(pid.getPrimaryLanguage()));
		patient.setDeceased(getType(pid.getPatientDeathIndicator()));
		patient.setGender(getGender(pid.getAdministrativeSex()));
		patient.setMaritalStatus(getMaritalStatus(pid.getMaritalStatus()));
		patient.setMultipleBirth(getType(pid.getMultipleBirthIndicator()));
		patient.setTelecom(createTelecoms(pid));

		patient.setExtension(createExtensions(pid));
		return patient;
	}

	private List<Identifier> createIdentifiers(PID pid, int tenantId) {
		List<Identifier> identifiers = new ArrayList<>();

		// TODO: handle duplicates. Identifier doesn't define hashCode or equals so a
		// Set doesn't affect uniqueness.

		for (CX patientIdentifier : pid.getPatientIdentifierList()) {
			Optional<Identifier> fhirIdentifier = createIdentifier(patientIdentifier);
			fhirIdentifier.ifPresent(identifiers::add);
		}

		Optional<Identifier> patientAccountNumber = createIdentifier(pid.getPatientAccountNumber());
		patientAccountNumber.ifPresent(identifiers::add);

		// TODO: SSN
		// TODO: Driver's License

		// For multi-tenancy purposes, we need to include the tenant ID as an identifier
		// for this patient.
		Identifier tenantIdentifier = new Identifier().setSystem("http://projectronin.com/fhir/tenant")
				.setValue(Integer.toString(tenantId));
		identifiers.add(tenantIdentifier);

		return identifiers;
	}

	private Optional<Identifier> createIdentifier(CX hl7Identifier) {
		String identifierTypeCode = hl7Identifier.getIdentifierTypeCode().getValue();

		Optional<CodeableConcept> type = IdentifierConstants.getCodeableConcept(identifierTypeCode);
		if (type.isPresent()) {
			String id = hl7Identifier.getIDNumber().getValue();
			Optional<String> system = IdentifierConstants.getSystem(identifierTypeCode);

			Identifier identifier = new Identifier().setSystem(system.orElse(null)).setValue(id).setType(type.get());
			return Optional.of(identifier);
		}

		return Optional.empty();
	}

	private List<HumanName> createNames(XPN[] hl7Names) throws HL7Exception, JsonProcessingException {
		List<HumanName> fhirNames = new ArrayList<>();
		for (XPN hl7Name : hl7Names) {
			HumanName fhirName = new HumanName();
			// TODO: Use Name Type Code to map "use"
			fhirName.setFamily(hl7Name.getFamilyName().getSurname().getValue());
			fhirName.addGiven(hl7Name.getGivenName().getValue());
			fhirName.addGiven(hl7Name.getSecondAndFurtherGivenNamesOrInitialsThereof().getValue());
			fhirName.addPrefix(hl7Name.getPrefixEgDR().getValue());
			fhirName.addSuffix(hl7Name.getSuffixEgJRorIII().getValue());
			fhirName.addSuffix(hl7Name.getProfessionalSuffix().getValue());

			Optional<Period> period = createPeriod(hl7Name.getEffectiveDate(), hl7Name.getExpirationDate());
			period.ifPresent(fhirName::setPeriod);

			fhirNames.add(fhirName);
		}
		return fhirNames;
	}

	private Optional<Period> createPeriod(DTM beginDate, DTM endDate) throws HL7Exception {
		if (beginDate.isEmpty()) {
			return Optional.empty();
		}

		Period period = new Period();
		period.setStart(createDate(beginDate));
		if (!endDate.isEmpty()) {
			period.setEnd(createDate(endDate));
		}

		return Optional.of(period);
	}

	private List<Address> createAddresses(XAD[] addresses) {
		// TODO
		return null;
	}

	private Date createDate(DTM dateTime) {
		// TODO
		return null;
	}

	private List<PatientCommunicationComponent> createCommunication(CWE primaryLanguage) {
		// TODO:
		return null;
	}

	private Type getType(ID indicator) {
		// TODO
		return null;
	}

	private AdministrativeGender getGender(CWE administrativeSex) {
		// TODO
		return null;
	}

	private CodeableConcept getMaritalStatus(CWE maritalStatus) {
		// TODO
		return null;
	}

	private List<ContactPoint> createTelecoms(PID pid) {
		// TODO
		return null;
	}

	private List<Extension> createExtensions(PID pid) {
		// TODO
		return null;
	}
}
