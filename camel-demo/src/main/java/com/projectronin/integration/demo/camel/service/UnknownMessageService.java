package com.projectronin.integration.demo.camel.service;

import java.io.IOException;

import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;

/**
 * Service for handling unknown messages.
 * 
 * @author Josh Smith
 */
@Component
public class UnknownMessageService {
	public Message process(Message input) throws IOException, HL7Exception {
		System.out.println("Received unknown input: ");
		System.out.println(input);
		return input.generateACK();
	}
}
