package it.nextworks.nfvmano.catalogue.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

public class NsdDeletionNotificationMessage extends CatalogueMessage {

	private final String nsdInfoId;
	private final String nsdId;

	@JsonCreator
	public NsdDeletionNotificationMessage(
			@JsonProperty("nsdInfoId") String nsdInfoId,
			@JsonProperty("nsdId") String nsdId,
			@JsonProperty("scope") ScopeType scope
	) {
		super(CatalogueMessageType.NSD_DELETION_NOTIFICATION, UUID.randomUUID(), scope, OperationStatus.SENT);
		this.nsdInfoId = nsdInfoId;
		this.nsdId = nsdId;
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
