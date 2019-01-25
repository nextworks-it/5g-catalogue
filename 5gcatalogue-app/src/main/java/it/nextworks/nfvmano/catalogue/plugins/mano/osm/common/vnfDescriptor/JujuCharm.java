package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class JujuCharm {

    private String charm;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getCharm() {
        return charm;
    }

    public void setCharm(String charm) {
        this.charm = charm;
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
        return "JujuCharm{" +
                "charm='" + charm + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
