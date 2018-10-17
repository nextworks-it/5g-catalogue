package it.nextworks.nfvmano.catalogue.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

/**
 * Created by Marco Capitani on 17/08/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
public class NsdChangeNotificationMessage extends CatalogueMessage {


    private final String nsdInfoId;
    private final String nsdId;
    private final NsdOperationalStateType operationalState;
    
    @JsonInclude(Include.NON_NULL)
	private String pluginId;

    @JsonCreator
    public NsdChangeNotificationMessage(
            @JsonProperty("nsdInfoId") String nsdInfoId,
            @JsonProperty("nsdId") String nsdId,
			@JsonProperty("operationId") UUID operationId,
            @JsonProperty("operationalState") NsdOperationalStateType operationalState,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
			@JsonProperty("notifierId") String pluginId
    ) {
    	super(CatalogueMessageType.NSD_CHANGE_NOTIFICATION, operationId, scope, opStatus);
        this.nsdInfoId = nsdInfoId;
        this.nsdId = nsdId;
        this.operationalState = operationalState;
        this.pluginId = pluginId;
    }

    @JsonProperty("nsdInfoId")
    public String getNsdInfoId() {
        return nsdInfoId;
    }

    @JsonProperty("nsdId")
    public String getNsdId() {
        return nsdId;
    }

    @JsonProperty("operationalState")
    public NsdOperationalStateType getOperationalState() {
        return operationalState;
    }
    
    @JsonProperty("notifierId")
	public String getPluginId() {
		return pluginId;
	}
}
