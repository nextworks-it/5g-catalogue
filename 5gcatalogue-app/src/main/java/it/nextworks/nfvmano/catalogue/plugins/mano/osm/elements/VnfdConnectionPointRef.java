package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class VnfdConnectionPointRef {

    public VnfdConnectionPointRef() {

    }

    private String vnfdIdRef;

    @JsonProperty("vnfd-id-ref")
    public String getVnfdIdRef() {
        return vnfdIdRef;
    }

    @JsonProperty("vnfd-id-ref")
    public VnfdConnectionPointRef setVnfdIdRef(String vnfdIdRef) {
        this.vnfdIdRef = vnfdIdRef;
        return this;
    }

    private String vnfdConnectionPointRef;

    @JsonProperty("vnfd-connection-point-ref")
    public String getVnfdConnectionPointRef() {
        return vnfdConnectionPointRef;
    }

    @JsonProperty("vnfd-connection-point-ref")
    public VnfdConnectionPointRef setVnfdConnectionPointRef(String vnfdConnectionPointRef) {
        this.vnfdConnectionPointRef = vnfdConnectionPointRef;
        return this;
    }

    private int memberVnfIndexRef;

    @JsonProperty("member-vnf-index-ref")
    public int getMemberVnfIndexRef() {
        return memberVnfIndexRef;
    }

    @JsonProperty("member-vnf-index-ref")
    public VnfdConnectionPointRef setMemberVnfIndexRef(int memberVnfIndexRef) {
        this.memberVnfIndexRef = memberVnfIndexRef;
        return this;
    }

}