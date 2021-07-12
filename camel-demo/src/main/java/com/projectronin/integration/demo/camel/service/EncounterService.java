package com.projectronin.integration.demo.camel.service;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Header;
import org.hl7.fhir.r4.model.Encounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectronin.integration.demo.mdaoc.MdaOcClient;

import ca.uhn.fhir.parser.IParser;

@Component
public class EncounterService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EncounterService.class);
	private final MdaOcClient mdaOcClient;
	private final IParser jsonParser;

	@Autowired
	public EncounterService(final MdaOcClient mdaOcClient, final IParser jsonParser) {
		this.mdaOcClient = mdaOcClient;
		this.jsonParser = jsonParser;
	}

	public void loadEncounters(@Header("fhirId") final String fhirId)
			throws RestClientException, URISyntaxException, InterruptedException {
		// Just a small delay from demo purposes.
		Thread.sleep(1000);

		// We're going to load encounters for the last week.
		LocalDate today = LocalDate.of(2021, 7, 8); // LocalDate.now();
		LocalDate lastWeek = today.minusDays(7);

		String encounterUrl = String.format("/oc/api/FHIR/STU3/Encounter?patient=%s&date=ge%s&date=le%s", fhirId,
				lastWeek.toString(), today.toString());
		List<Encounter> encounters = loadEncountersFromUrl(encounterUrl);
		LOGGER.info("Found {} encounters", encounters.size());

		for (Encounter encounter : encounters) {
			LOGGER.info("Encounter {} occurred at {}", encounter.getId(), encounter.getPeriod().getStart().toString());
		}
	}

	private List<Encounter> loadEncountersFromUrl(final String url) throws RestClientException, URISyntaxException {
		ResponseEntity<JsonNode> response = mdaOcClient.get(url, JsonNode.class);
		if (response.getStatusCode() != HttpStatus.OK) {
			throw new RuntimeException(
					String.format("Unable to retrieve Encounters. Status code = %s", response.getStatusCodeValue()));
		}

		List<Encounter> encounters = new ArrayList<>();
		JsonNode root = response.getBody();
		JsonNode entries = root.get("entry");
		for (JsonNode entry : entries) {
			JsonNode resource = entry.get("resource");
			Encounter encounter = jsonParser.parseResource(Encounter.class, resource.toString());
			encounters.add(encounter);
		}

		JsonNode links = root.get("link");
		for (JsonNode link : links) {
			String relation = link.get("relation").textValue();
			if ("next".equals(relation)) {
				String nextUrl = link.get("url").textValue();
				encounters.addAll(loadEncountersFromUrl(nextUrl));

				// There should only be 1 next.
				break;
			}
		}
		return encounters;
	}
}
