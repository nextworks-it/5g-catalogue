package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class InternalConnectionPointVld {

    @JsonProperty("id-ref")
    private String idRef;
    @JsonProperty("ip-address")
    private String ip;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getIdRef() {
        return idRef;
    }

    public void setIdRef(String idRef) {
        this.idRef = idRef;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
                "idRef='" + idRef + '\'' +
                ", ip='" + ip + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
