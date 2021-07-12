package com.projectronin.integration.demo.camel.route;

import static org.apache.camel.ExchangePattern.InOnly;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.projectronin.integration.demo.camel.extractor.FhirIdExtractor;
import com.projectronin.integration.demo.camel.extractor.MessageTypeExtractor;
import com.projectronin.integration.demo.camel.extractor.TenantExtractor;
import com.projectronin.integration.demo.camel.service.EncounterService;

/**
 * Router for handling HL7 requests
 *
 * @author Josh Smith
 */
@Component
public class HL7Router extends RouteBuilder {
	private final MessageTypeExtractor messageTypeExtractor;
	private final TenantExtractor tenantExtractor;
	private final FhirIdExtractor fhirIdExtractor;
	private final EncounterService encounterService;

	@Autowired
	public HL7Router(final MessageTypeExtractor messageTypeExtractor, final TenantExtractor tenantExtractor,
			final FhirIdExtractor fhirIdExtractor, final EncounterService encounterService) {
		this.messageTypeExtractor = messageTypeExtractor;
		this.tenantExtractor = tenantExtractor;
		this.fhirIdExtractor = fhirIdExtractor;
		this.encounterService = encounterService;
	}

	@Override
	public void configure() throws Exception {
		/*
		//@formatter:off
		from("netty:tcp://localhost:8888")
			.routeId("netty-hl7")
			.setHeader("messageType", messageTypeExtractor)
			.setHeader("tenant", tenantExtractor)
			.filter(header("tenant").isNull())
				.log("Unknown tenant. Acknowledging request and ending processing")
				.transform(HL7.ack())
				.stop()
				.end()
			.log("Processing ${header.messageType} for tenant ${header.tenant}")
			.choice()
				.when(header("messageType").isEqualTo("ADT^A01"))
					.bean(AdmitService.class)
				.otherwise()
					.bean(UnknownMessageService.class)
			.endChoice();
		//@formatter:on
		*/

		//@formatter:off
		from("netty:tcp://localhost:8888")
			.routeId("netty-hl7")
			.setHeader("messageType", messageTypeExtractor)
			.setHeader("tenant", tenantExtractor)
			.filter(header("tenant").isNull())
				.log("Unknown tenant. Acknowledging request and ending processing")
				.stop()
				.end()
			.log("Processing ${header.messageType} for tenant ${header.tenant}.")
			.to(InOnly, "activemq:fhir") // "InOnly" allows us to place the message on the queue and continue processing.
			.transform(HL7.ack());

		from("activemq:fhir")
			.routeId("fhirLookup")
			.setHeader("fhirId", fhirIdExtractor)
			.filter(header("fhirId").isNull())
				.log("No FHIR ID present for patient")
				.stop()
				.end()
			.log("FHIR STU3 ID extracted and adding to queue")
			.to(InOnly, "activemq:encounter");

		from("activemq:encounter")
			.routeId("encounterLookup")
			.log("Received message for FHIR STU3 ID ${header.fhirId}")
			.bean(encounterService);
		//@formatter:on
	}
}