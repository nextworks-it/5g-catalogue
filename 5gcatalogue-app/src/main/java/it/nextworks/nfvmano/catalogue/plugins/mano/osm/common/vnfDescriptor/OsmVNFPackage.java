package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OsmVNFPackage {

    @JsonProperty("vnfd:vnfd-catalog")
    private VNFDCatalog vnfdCatalog;

    public OsmVNFPackage() { }


    public VNFDCatalog getVnfdCatalog() {
            return vnfdCatalog;
        }

    public OsmVNFPackage setVnfdCatalog(VNFDCatalog vnfdCatalog) {
        this.vnfdCatalog = vnfdCatalog;
        return this;
    }

    @Override
    public String toString() {
        return "TopLevelContainer{" +
                "catalog=" + vnfdCatalog +
                '}';
    }
}
