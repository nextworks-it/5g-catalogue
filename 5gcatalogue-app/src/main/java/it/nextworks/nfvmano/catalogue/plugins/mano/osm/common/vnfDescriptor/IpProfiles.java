package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class IpProfiles {

    private String name;
    private String description;
    @JsonProperty("ip-profile-params")
    private IpProfileParams ipProfileParams;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IpProfileParams getIpProfileParams() {
        return ipProfileParams;
    }

    public void setIpProfileParams(IpProfileParams ipProfileParams) {
        this.ipProfileParams = ipProfileParams;
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
        return "IpProfiles{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", ipProfileParams=" + ipProfileParams +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
