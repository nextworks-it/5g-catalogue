package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class VnfMetric {
    @JsonProperty("vnf-metric-name-ref")
    private String vnfMetricNameRef;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getVnfMetricNameRef() {
        return vnfMetricNameRef;
    }

    public void setVnfMetricNameRef(String vnfMetricNameRef) {
        this.vnfMetricNameRef = vnfMetricNameRef;
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
        return "VnfMetric{" +
                "vnfMetricNameRef='" + vnfMetricNameRef + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
