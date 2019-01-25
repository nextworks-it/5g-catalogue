package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Script {

    @JsonProperty("script-type")
    public String scriptType;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
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
        return "Script{" +
                "scriptType='" + scriptType + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
