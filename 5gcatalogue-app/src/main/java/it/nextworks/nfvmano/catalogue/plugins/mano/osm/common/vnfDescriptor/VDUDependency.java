package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class VDUDependency {

    @JsonProperty("vdu-source-ref")
    private String vduSourceRef;
    @JsonProperty("vdu-depends-on-ref")
    private String vduDependsOnRef;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getVduSourceRef() {
        return vduSourceRef;
    }

    public void setVduSourceRef(String vduSourceRef) {
        this.vduSourceRef = vduSourceRef;
    }

    public String getVduDependsOnRef() {
        return vduDependsOnRef;
    }

    public void setVduDependsOnRef(String vduDependsOnRef) {
        this.vduDependsOnRef = vduDependsOnRef;
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
        return "VDUDependency{" +
                "vduSourceRef='" + vduSourceRef + '\'' +
                ", vduDependsOnRef='" + vduDependsOnRef + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
