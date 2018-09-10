package it.nextworks.nfvmano.catalogue.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "msgType")
@JsonSubTypes({
        @Type(
                value = NsdOnBoardingNotificationMessage.class,
                name = "NSD_ONBOARDING_NOTIFICATION"
        ),
        @Type(
                value = NsdChangeNotificationMessage.class,
                name = "NSD_CHANGE_NOTIFICATION"
        ),
        @Type(
                value = NsdDeletionNotificationMessage.class,
                name = "NSD_DELETE_NOTIFICATION"
        )
})

public class CatalogueMessage {

    CatalogueMessageType type;
    UUID operationId;
    ScopeType scope;
    OperationStatus opStatus;
    
    @JsonCreator
    public CatalogueMessage(@JsonProperty("operationId") UUID operationId, 
    		@JsonProperty("scope") ScopeType scope,
    		@JsonProperty("opStatus") OperationStatus opStatus) {
    	this.operationId = operationId;
    	this.scope = scope;
    	this.opStatus = opStatus;
    }

    @JsonIgnore
    public CatalogueMessageType getType() {
        return type;
    }

    @JsonProperty("operationId")
	public UUID getOperationId() {
		return operationId;
	}
    
    @JsonProperty("scope")
    public ScopeType getScope() {
    	return scope;
    }

    @JsonProperty("opStatus")
	public OperationStatus getOpStatus() {
		return opStatus;
	}

	public void setOpStatus(OperationStatus opStatus) {
		this.opStatus = opStatus;
	}
}
