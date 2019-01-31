package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class InternalConnectionPoint {

    private String id;
    private String name;
    @JsonProperty("short-name")
    private String shortName;
    private String type;
    @JsonProperty("port-security-enabled")
    private boolean portSecuityEnabled;
    @JsonProperty("internal-vld-ref")
    private String internalVldRef;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPortSecuityEnabled() {
        return portSecuityEnabled;
    }

    public void setPortSecuityEnabled(boolean portSecuityEnabled) {
        this.portSecuityEnabled = portSecuityEnabled;
    }

    public String getInternalVldRef() {
        return internalVldRef;
    }

    public void setInternalVldRef(String internalVldRef) {
        this.internalVldRef = internalVldRef;
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
        return "InternalConnectionPoint{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", type='" + type + '\'' +
                ", portSecuityEnabled=" + portSecuityEnabled +
                ", internalVldRef='" + internalVldRef + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
