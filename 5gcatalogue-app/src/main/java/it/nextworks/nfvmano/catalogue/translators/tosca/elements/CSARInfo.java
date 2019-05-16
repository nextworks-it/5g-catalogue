package it.nextworks.nfvmano.catalogue.translators.tosca.elements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

public class CSARInfo {

    @JsonIgnore
    private DescriptorTemplate mst;

    @JsonProperty("packageFilename")
    private String packageFilename;

    @JsonProperty("descriptorFilename")
    private String descriptorFilename;

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

    public String getPackageFilename() {
        return packageFilename;
    }

    public void setPackageFilename(String packageFilename) {
        this.packageFilename = packageFilename;
    }

    public String getDescriptorFilename() {
        return descriptorFilename;
    }

    public void setDescriptorFilename(String descriptorFilename) {
        this.descriptorFilename = descriptorFilename;
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
