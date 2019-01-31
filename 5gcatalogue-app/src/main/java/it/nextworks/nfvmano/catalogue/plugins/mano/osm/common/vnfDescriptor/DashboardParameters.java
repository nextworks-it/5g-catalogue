package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class DashboardParameters {

    private String path;
    private boolean https;
    private Integer port;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) { this.path = path; }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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
        return "DashboardParameters{" +
                "path='" + path + '\'' +
                ", https=" + https +
                ", port=" + port +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
