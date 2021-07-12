package com.projectronin.integration.demo.camel.extractor;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.google.common.base.Strings;
import com.projectronin.integration.demo.mdaoc.MdaOcClient;
import com.projectronin.integration.demo.mdaoc.model.PatientIdentifier;
import com.projectronin.integration.demo.mdaoc.model.PatientIdentifiers;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.GenericSegment;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.util.Terser;

@Component
public class FhirIdExtractor implements Expression {
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirIdExtractor.class);
	private final MdaOcClient mdaOcClient;

	@Autowired
	public FhirIdExtractor(final MdaOcClient mdaOcClient) {
		this.mdaOcClient = mdaOcClient;
	}

	@Override
	public <T> T evaluate(final Exchange exchange, final Class<T> type) {
		try {
			// Just a small delay from demo purposes.
			Thread.sleep(1000);
			Map<String, String> identifiersByType = getIdentifiersByType(exchange.getIn().getBody(Message.class));
			LOGGER.info("Loaded {} identifiers", identifiersByType.size());

			String mrn = identifiersByType.get("MR");
			LOGGER.info("Loaded MRN {}", mrn);
			String fhirId = getFhirID(mrn).orElse(null);
			LOGGER.info("Loaded FHIR STU3 ID {}", fhirId);

			return exchange.getContext().getTypeConverter().convertTo(type, fhirId);
		} catch (HL7Exception | RestClientException | URISyntaxException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, String> getIdentifiersByType(final Message message) throws HL7Exception {
		Map<String, String> identifiersByType = new HashMap<>();

		GenericSegment pid = (GenericSegment) message.get("PID");

		// Patient ID
		Varies pid2 = (Varies) pid.getField(2, 0);
		addIdentifier(pid2, identifiersByType);

		// Patient IDs
		for (Type pid3 : pid.getField(3)) {
			addIdentifier((Varies) pid3, identifiersByType);
		}

		// Alternate Patient IDs
		for (Type pid4 : pid.getField(4)) {
			addIdentifier((Varies) pid4, identifiersByType);
		}

		// Patient Account Number
		Varies pid18 = (Varies) pid.getField(18, 0);
		addIdentifier(pid18, identifiersByType);

		return identifiersByType;
	}

	private void addIdentifier(final Varies identifierObject, final Map<String, String> identifiersByType) {
		String type = Terser.getPrimitive(identifierObject, 5, 1).getValue();
		if (type == null) {
			// If we don't know a type, it's of no use to us.
			return;
		}
		if (identifiersByType.containsKey(type)) {
			LOGGER.info("Received second id for type {}", type);
		}

		String identifier = Terser.getPrimitive(identifierObject, 1, 1).getValue();
		identifiersByType.put(type, identifier);
	}

	private Optional<String> getFhirID(final String mrn) throws RestClientException, URISyntaxException {
		if (mrn == null) {
			return null;
		}

		String paddedMrn = Strings.padStart(mrn, 7, '0');
		String path = String.format("/oc/patient/%s/identifiers/type/MRN", paddedMrn);

		ResponseEntity<PatientIdentifiers> response = mdaOcClient.get(path, PatientIdentifiers.class);
		if (response.getStatusCode() != HttpStatus.OK) {
			throw new IllegalStateException(String.format(
					"Unable to validate MRN and retrieve FHIR ID. Server returned %s", response.getStatusCodeValue()));
		}

		PatientIdentifiers patientIdentifiers = response.getBody();
		LOGGER.info("Retrieved {} identifiers from MRN", patientIdentifiers.getIdentifiers().size());
		return patientIdentifiers.getIdentifiers().stream().filter(i -> "FHIR STU3".equalsIgnoreCase(i.getIdType()))
				.findFirst().map(PatientIdentifier::getId);
	}
}
