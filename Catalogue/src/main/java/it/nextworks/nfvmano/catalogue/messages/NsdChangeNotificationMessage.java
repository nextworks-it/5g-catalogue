package it.nextworks.nfvmano.catalogue.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;

/**
 * Created by Marco Capitani on 17/08/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
public class NsdChangeNotificationMessage extends CatalogueMessage {


    private final String nsdInfoId;
    private final String nsdId;
    private final NsdOperationalStateType operationalState;

    @JsonCreator
    public NsdChangeNotificationMessage(
            @JsonProperty("nsdInfoId") String nsdInfoId,
            @JsonProperty("nsdId") String nsdId,
            @JsonProperty("operationalState") NsdOperationalStateType operationalState
    ) {
        this.nsdInfoId = nsdInfoId;
        this.nsdId = nsdId;
        this.operationalState = operationalState;
        this.type = CatalogueMessageType.NSD_CHANGE_NOTIFICATION;
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
}
