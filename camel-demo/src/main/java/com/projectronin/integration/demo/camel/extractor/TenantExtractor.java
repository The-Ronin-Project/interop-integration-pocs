package com.projectronin.integration.demo.camel.extractor;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.projectronin.integration.demo.dao.TenantDAO;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.GenericSegment;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.util.Terser;

/**
 * Extracts a Tenant from an Exchange.
 * 
 * @author Josh Smith
 */
@Component
public class TenantExtractor implements Expression {
	private TenantDAO tenantDAO;

	@Autowired
	public TenantExtractor(TenantDAO tenantDAO) {
		this.tenantDAO = tenantDAO;
	}

	@Override
	public <T> T evaluate(Exchange exchange, Class<T> type) {
		try {
			String hl7FacilityId = getHL7Facility(exchange.getIn().getBody(Message.class));

			Optional<Integer> tenantId = tenantDAO.getTenantId(hl7FacilityId);
			String tenantIdString = tenantId.map(i -> Integer.toString(i)).orElse(null);
			return exchange.getContext().getTypeConverter().convertTo(type, tenantIdString);
		} catch (HL7Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getHL7Facility(Message message) throws HL7Exception {
		GenericSegment msh = (GenericSegment) message.get("MSH");
		Varies messageType = (Varies) msh.getField(4)[0];

		return Terser.getPrimitive(messageType.getData(), 1, 1).getValue();
	}
}
