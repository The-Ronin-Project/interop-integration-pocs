package com.projectronin.integration.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.GenericSegment;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v281.segment.MSH;
import ca.uhn.hl7v2.util.Terser;

/**
 * Extracts a Tenant from an Exchange.
 * 
 * @author Josh Smith
 */
public class TenantExtractor {
	private Map<String, Integer> tenantIdsByFacility = new HashMap<>();

	public TenantExtractor() {
		tenantIdsByFacility.put("1", 1001);
		tenantIdsByFacility.put("MDA", 1002);
		tenantIdsByFacility.put("PSJ", 1003);
	}

	public Optional<Integer> getTenantId(Message message) throws HL7Exception {
		String hl7FacilityId = getHL7Facility(message);
		if (hl7FacilityId == null) {
			return Optional.empty();
		}

		Integer tenantId = tenantIdsByFacility.get(hl7FacilityId);
		return Optional.ofNullable(tenantId);
	}

	private String getHL7Facility(Message message) throws HL7Exception {
		Object msh = message.get("MSH");
		if (msh instanceof MSH) {
			return ((MSH) msh).getSendingFacility().getNamespaceID().getValue();
		} else if (msh instanceof GenericSegment) {
			Varies messageType = (Varies) ((GenericSegment) msh).getField(4)[0];

			return Terser.getPrimitive(messageType.getData(), 1, 1).getValue();
		}

		return null;
	}

}
