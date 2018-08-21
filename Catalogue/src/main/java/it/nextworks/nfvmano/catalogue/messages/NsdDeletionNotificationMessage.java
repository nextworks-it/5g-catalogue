package it.nextworks.nfvmano.catalogue.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NsdDeletionNotificationMessage extends CatalogueMessage {

	private final String nsdInfoId;
	private final String nsdId;

	@JsonCreator
	public NsdDeletionNotificationMessage(
			@JsonProperty("nsdInfoId") String nsdInfoId,
			@JsonProperty("nsdId") String nsdId
	) {
		this.nsdInfoId = nsdInfoId;
		this.nsdId = nsdId;
		this.type = CatalogueMessageType.NSD_DELETION_NOTIFICATION;
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
