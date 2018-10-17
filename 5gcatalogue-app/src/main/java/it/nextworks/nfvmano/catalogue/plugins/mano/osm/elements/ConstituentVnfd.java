package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class ConstituentVnfd {

    public ConstituentVnfd() {

    }

    private int memberVnfIndex;

    @JsonProperty("member-vnf-index")
    public int getMemberVnfIndex() {
        return memberVnfIndex;
    }

    @JsonProperty("member-vnf-index")
    public ConstituentVnfd setMemberVnfIndex(int memberVnfIndex) {
        this.memberVnfIndex = memberVnfIndex;
        return this;
    }

    private String vnfdIdRef;

    @JsonProperty("vnfd-id-ref")
    public String getVnfdIdRef() {
        return vnfdIdRef;
    }

    @JsonProperty("vnfd-id-ref")
    public ConstituentVnfd setVnfdIdRef(String vnfdIdRef) {
        this.vnfdIdRef = vnfdIdRef;
        return this;
    }

}