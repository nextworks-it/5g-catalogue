package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class VduMonitoringParam {

    @JsonProperty("vdu-ref")
    private String vduRef;
    @JsonProperty("vdu-monitoring-param-ref")
    private String vduMonitoringParamRef;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getVduRef() {
        return vduRef;
    }

    public void setVduRef(String vduRef) {
        this.vduRef = vduRef;
    }

    public String getVduMonitoringParamRef() {
        return vduMonitoringParamRef;
    }

    public void setVduMonitoringParamRef(String vduMonitoringParamRef) {
        this.vduMonitoringParamRef = vduMonitoringParamRef;
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
        return "VduMonitoringParam{" +
                "vduRef='" + vduRef + '\'' +
                ", vduMonitoringParamRef='" + vduMonitoringParamRef + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}