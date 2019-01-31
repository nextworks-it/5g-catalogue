package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ProviderNetwork {

    @JsonProperty("physical-network")
    private String physicalNet;
    @JsonProperty("segmentation-id")
    private Integer segId;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getPhysicalNet() {
        return physicalNet;
    }

    public void setPhysicalNet(String physicalNet) {
        this.physicalNet = physicalNet;
    }

    public Integer getSegId() {
        return segId;
    }

    public void setSegId(Integer segId) {
        this.segId = segId;
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return otherProperties;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        otherProperties.put(name, value);
    }

    @Override
    public String toString() {
        return "ProviderNetwork{" +
                "physicalNet='" + physicalNet + '\'' +
                ", segId=" + segId +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
