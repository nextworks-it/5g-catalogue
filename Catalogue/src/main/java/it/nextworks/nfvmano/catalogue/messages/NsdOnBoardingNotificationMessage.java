package it.nextworks.nfvmano.catalogue.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NsdOnBoardingNotificationMessage extends CatalogueMessage {
	
	private String nsdName;
	
	@JsonCreator
	public NsdOnBoardingNotificationMessage(@JsonProperty("nsdName") String nsdName) {
		this.nsdName = nsdName;
		this.type = CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION;
	}

	@JsonProperty("nsdName")
	public String getNsdName() {
		return nsdName;
	}
}
