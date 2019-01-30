package it.nextworks.nfvmano.catalogue.translators.tosca.elements;

import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

public class CSARInfo {

    private DescriptorTemplate mst;

    private String vnfPkgFilename;

    private String vnfdFilename;

    private String metaFilename;

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
