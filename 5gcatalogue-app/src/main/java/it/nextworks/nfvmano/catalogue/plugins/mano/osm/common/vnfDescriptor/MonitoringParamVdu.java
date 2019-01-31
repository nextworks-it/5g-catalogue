package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class MonitoringParamVdu {

    private String id;
    @JsonProperty("nfvi-metric")
    private String nfviMetric;
    @JsonProperty("interface-name-ref")
    private String interfaceNameRef;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNfviMetric() {
        return nfviMetric;
    }

    public void setNfviMetric(String nfviMetric) {
        this.nfviMetric = nfviMetric;
    }

    public String getInterfaceNameRef() {
        return interfaceNameRef;
    }

    public void setInterfaceNameRef(String interfaceNameRef) {
        this.interfaceNameRef = interfaceNameRef;
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
        return "MonitoringParamVdu{" +
                "id='" + id + '\'' +
                ", nfviMetric='" + nfviMetric + '\'' +
                ", interfaceNameRef='" + interfaceNameRef + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}