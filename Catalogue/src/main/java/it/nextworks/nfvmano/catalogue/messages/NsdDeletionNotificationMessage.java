package it.nextworks.nfvmano.catalogue.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

public class NsdDeletionNotificationMessage extends CatalogueMessage {

	private final String nsdInfoId;
	private final String nsdId;
	
	@JsonInclude(Include.NON_NULL)
	private String pluginId;

	@JsonCreator
	public NsdDeletionNotificationMessage(
			@JsonProperty("nsdInfoId") String nsdInfoId,
			@JsonProperty("nsdId") String nsdId,
			@JsonProperty("operationId") UUID operationId,
			@JsonProperty("scope") ScopeType scope,
			@JsonProperty("operationStatus") OperationStatus opStatus,
			@JsonProperty("notifierId") String pluginId
	) {
		super(CatalogueMessageType.NSD_DELETION_NOTIFICATION, operationId, scope, opStatus);
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
	
	@JsonProperty("notifierId")
	public String getPluginId() {
		return pluginId;
	}
}
