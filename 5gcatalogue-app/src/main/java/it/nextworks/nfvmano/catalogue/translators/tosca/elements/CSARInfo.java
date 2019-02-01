package it.nextworks.nfvmano.catalogue.translators.tosca.elements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

public class CSARInfo {

    @JsonIgnore
    private DescriptorTemplate mst;

    @JsonProperty("vnfPkgFilename")
    private String vnfPkgFilename;

    @JsonProperty("vnfdFilename")
    private String vnfdFilename;

    @JsonProperty("metaFilename")
    private String metaFilename;

    @JsonProperty("mfFilename")
    private String mfFilename;

    public CSARInfo() {
    }

    public DescriptorTemplate getMst() {
        return mst;
    }

    public void setMst(DescriptorTemplate mst) {
        this.mst = mst;
    }

    public String getVnfPkgFilename() {
        return vnfPkgFilename;
    }

    public void setVnfPkgFilename(String vnfPkgFilename) {
        this.vnfPkgFilename = vnfPkgFilename;
    }

    public String getVnfdFilename() {
        return vnfdFilename;
    }

    public void setVnfdFilename(String vnfdFilename) {
        this.vnfdFilename = vnfdFilename;
    }

    public String getMetaFilename() {
        return metaFilename;
    }

    public void setMetaFilename(String metaFilename) {
        this.metaFilename = metaFilename;
    }

    public String getMfFilename() {
        return mfFilename;
    }

    public void setMfFilename(String mfFilename) {
        this.mfFilename = mfFilename;
    }
}
