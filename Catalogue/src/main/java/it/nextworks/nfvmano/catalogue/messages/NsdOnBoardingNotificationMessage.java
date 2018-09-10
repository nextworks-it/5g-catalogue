package it.nextworks.nfvmano.catalogue.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

public class NsdOnBoardingNotificationMessage extends CatalogueMessage {
	
	private final String nsdInfoId;
	private final String nsdId;
	
	@JsonInclude(Include.NON_NULL)
	private String pluginId;
	
	
	@JsonCreator
	public NsdOnBoardingNotificationMessage(
			@JsonProperty("nsdInfoId") String nsdInfoId,
			@JsonProperty("nsdId") String nsdId,
			@JsonProperty("operationId") UUID operationId,
			@JsonProperty("scope") ScopeType scope
	) {
		super(operationId, scope, OperationStatus.SENT);
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

	@JsonProperty("pluginId")
	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String manoId) {
		this.pluginId = manoId;
	}
}
