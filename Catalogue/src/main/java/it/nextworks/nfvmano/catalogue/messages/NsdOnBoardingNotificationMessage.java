package it.nextworks.nfvmano.catalogue.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NsdOnBoardingNotificationMessage extends CatalogueMessage {
	
	private final String nsdInfoId;
	private final String nsdId;
	
	@JsonCreator
	public NsdOnBoardingNotificationMessage(
			@JsonProperty("nsdInfoId") String nsdInfoId,
			@JsonProperty("nsdId") String nsdId
	) {
		this.nsdInfoId = nsdInfoId;
		this.nsdId = nsdId;
		this.type = CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION;
	}

	@JsonProperty("nsdInfoId")
	public String getNsdInfoId() {
		return nsdInfoId;
	}

	@JsonProperty("nsdId")
	public String getNsdId() {
		return nsdId;
	}
}
