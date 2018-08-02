package it.nextworks.nfvmano.catalogue.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "msgType")
@JsonSubTypes({
	@Type(value = NsdOnBoardingNotificationMessage.class, 				name = "NSD_ONBOARDING_NOTIFICATION")})

public class CatalogueMessage {

	CatalogueMessageType type;
	
	@JsonIgnore
	public CatalogueMessageType getType() {
		return type;
	}
}
