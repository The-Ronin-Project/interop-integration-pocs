package com.projectronin.integration.demo.camel.extractor;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.GenericSegment;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.util.Terser;

/**
 * Extracts a Message Type from an Exchange.
 * 
 * @author Josh Smith
 */
@Component
public class MessageTypeExtractor implements Expression {

	@Override
	public <T> T evaluate(Exchange exchange, Class<T> type) {
		try {
			String messageType = getMessageType(exchange.getIn().getBody(Message.class));
			return exchange.getContext().getTypeConverter().convertTo(type, messageType);
		} catch (HL7Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getMessageType(Message message) throws HL7Exception {
		GenericSegment msh = (GenericSegment) message.get("MSH");
		Varies messageType = (Varies) msh.getField(9)[0];

		String messageTypeId = Terser.getPrimitive(messageType.getData(), 1, 1).getValue();
		String triggerEventId = Terser.getPrimitive(messageType.getData(), 2, 1).getValue();
		return String.format("%s^%s", messageTypeId, triggerEventId);
	}
}
