package it.nextworks.nfvmano.libs.osmr10DataModels.nsd;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.descriptors.sol006.Nsd;

public class OsmNsdSol006 extends Nsd {

    @JsonProperty("description")
    private String description;

    public OsmNsdSol006(Nsd nsd) {
        setCertificate(nsd.getCertificate());
        setAlgorithm(nsd.getAlgorithm());
        setSignature(nsd.getSignature());
        setNestedNsdId(nsd.getNestedNsdId());
        setDesigner(nsd.getDesigner());
        setName(nsd.getName());
        setSapd(nsd.getSapd());
        setLifecycleManagementScript(nsd.getLifecycleManagementScript());
        setVirtualLinkDesc(nsd.getVirtualLinkDesc());
        setInvariantId(nsd.getInvariantId());
        setVersion(nsd.getVersion());
        setAutoscaleRule(nsd.getAutoscaleRule());
        setVnffgd(nsd.getVnffgd());
        setDf(nsd.getDf());
        setPnfdId(nsd.getPnfdId());
        setVnfdId(nsd.getVnfdId());
        setId(nsd.getId());
    }

    public OsmNsdSol006 description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OsmNsdSol006 {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    ").append(toIndentedString(description)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(java.lang.Object o) {
        if (o == null)
            return "null";

        return o.toString().replace("\n", "\n    ");
    }
}
