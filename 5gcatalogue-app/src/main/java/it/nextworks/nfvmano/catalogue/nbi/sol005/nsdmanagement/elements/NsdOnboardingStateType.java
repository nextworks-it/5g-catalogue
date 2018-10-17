package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The enumeration NsdOnboardingStateType shall comply with the provisions
 * defined in Table 5.5.4.5-1 of GS NFV-SOL 005. It indicates the onboarding
 * state of the NSD. CREATED = The NSD information object is created. UPLOADING
 * = The associated NSD content is being uploaded. PROCESSING = The associated
 * NSD content is being processed, e.g. validation. ONBOARDED = The associated
 * NSD content is on-boarded.
 */
public enum NsdOnboardingStateType {

	CREATED("CREATED"),

	UPLOADING("UPLOADING"),

	PROCESSING("PROCESSING"),

	ONBOARDED("ONBOARDED");

	private String value;

	NsdOnboardingStateType(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static NsdOnboardingStateType fromValue(String text) {
		for (NsdOnboardingStateType b : NsdOnboardingStateType.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
}
