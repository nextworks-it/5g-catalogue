package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class InitialConfigurationPrimitive {

    private Integer seq;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
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
        return "InitialConfigurationPrimitive{" +
                "seq=" + seq +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
