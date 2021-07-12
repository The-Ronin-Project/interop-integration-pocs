package com.projectronin.integration.demo.mdaoc.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as = ImmutablePatientIdentifiers.class)
@JsonDeserialize(as = ImmutablePatientIdentifiers.class)
public interface PatientIdentifiers {
	List<PatientIdentifier> getIdentifiers();
}
