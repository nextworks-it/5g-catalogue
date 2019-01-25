package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import java.util.List;

public class VNFDCatalog {

    public List<VNFDescriptor> vnfd;

    public List<VNFDescriptor> getVnfd() {
        return vnfd;
    }

    public void setVnfd(List<VNFDescriptor> vnfd) {
        this.vnfd = vnfd;
    }

    @Override
    public String toString() {
        return "VNFDCatalog{" +
                "vnfDescriptors=" + vnfd +
                '}';
    }
}
