package com.projectronin.integration.demo.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * DAO for retrieving tenants.
 * 
 * @author Josh Smith
 */
@Repository
public class TenantDAO {
	private Map<String, Integer> tenantIdsByFacility = new HashMap<>();

	@Autowired
	public TenantDAO() {
		tenantIdsByFacility.put("1", 1001);
		tenantIdsByFacility.put("MDA", 1002);
		tenantIdsByFacility.put("PSJ", 1003);
	}

	public Optional<Integer> getTenantId(String hl7FacilityId) {
		return Optional.ofNullable(tenantIdsByFacility.get(hl7FacilityId));
	}
}
