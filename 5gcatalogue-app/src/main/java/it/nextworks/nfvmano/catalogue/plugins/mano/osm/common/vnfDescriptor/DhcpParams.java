package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class DhcpParams {

    private boolean enabled;
    @JsonProperty("start-address")
    private String startAddress;
    private Integer count;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
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
        return "DhcpParams{" +
                "enabled=" + enabled +
                ", startAddress='" + startAddress + '\'' +
                ", count=" + count +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
