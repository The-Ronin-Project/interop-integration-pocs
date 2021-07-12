package com.projectronin.integration.demo.mdaoc.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as = ImmutablePatientIdentifier.class)
@JsonDeserialize(as = ImmutablePatientIdentifier.class)
public interface PatientIdentifier {
	String getId();

	String getIdType();
}
