package it.nextworks.nfvmano.libs.osmr10DataModels.vnfd;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.descriptors.sol006.Vnfd;

public class OsmVnfdSol006 extends Vnfd {

    @JsonProperty("description")
    private String description;

    @JsonProperty("mgmt-cp")
    private String mgmtCp;

    public OsmVnfdSol006(Vnfd vnfd) {
        setSecurityGroupRule(vnfd.getSecurityGroupRule());
        setDefaultLocalizationLanguage(vnfd.getDefaultLocalizationLanguage());
        setExtCpd(vnfd.getExtCpd());
        setId(vnfd.getId());
        setSwImageDesc(vnfd.getSwImageDesc());
        setElementGroup(vnfd.getElementGroup());
        setVirtualStorageDesc(vnfd.getVirtualStorageDesc());
        setIndicator(vnfd.getIndicator());
        setVirtualComputeDesc(vnfd.getVirtualComputeDesc());
        setVnfmInfo(vnfd.getVnfmInfo());
        setProductInfoName(vnfd.getProductInfoName());
        setModifiableAttributes(vnfd.getModifiableAttributes());
        setVersion(vnfd.getVersion());
        setProvider(vnfd.getProvider());
        setProductName(vnfd.getProductName());
        setDf(vnfd.getDf());
        setSoftwareVersion(vnfd.getSoftwareVersion());
        setConfigurableProperties(vnfd.getConfigurableProperties());
        setAutoScale(vnfd.getAutoScale());
        setLifecycleManagementScript(vnfd.getLifecycleManagementScript());
        setVdu(vnfd.getVdu());
        setLocalizationLanguage(vnfd.getLocalizationLanguage());
        setIntVirtualLinkDesc(vnfd.getIntVirtualLinkDesc());
        setProductInfoDescription(vnfd.getProductInfoDescription());
    }

    public OsmVnfdSol006 description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public OsmVnfdSol006 mgmtCp(String mgmtCp) {
        this.mgmtCp = mgmtCp;
        return this;
    }

    public String getMgmtCp() { return mgmtCp;}

    public void setMgmtCp(String mgmtCp) { this.mgmtCp = mgmtCp; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OsmVnfdSol006 {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    ").append(toIndentedString(description)).append("\n");
        sb.append("    ").append(toIndentedString(mgmtCp)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(java.lang.Object o) {
        if (o == null)
            return "null";

        return o.toString().replace("\n", "\n    ");
    }
}
