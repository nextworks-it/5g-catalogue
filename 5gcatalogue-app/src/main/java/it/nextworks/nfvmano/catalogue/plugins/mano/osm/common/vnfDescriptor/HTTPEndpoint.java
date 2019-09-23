package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.osmr4PlusClient.nsdManagement.elements.KeyValuePairs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPEndpoint {

    private String path;
    private boolean https;
    private Integer port;
    private String username;
    private String password;
    @JsonProperty("polling_interval_secs")
    private Integer pollingIntervalSecs;
    private String method;
    private List<KeyValuePairs> headers;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPollingIntervalSecs() {
        return pollingIntervalSecs;
    }

    public void setPollingIntervalSecs(Integer pollingIntervalSecs) {
        this.pollingIntervalSecs = pollingIntervalSecs;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<KeyValuePairs> getHeaders() {
        return headers;
    }

    public void setHeaders(List<KeyValuePairs> headers) {
        this.headers = headers;
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
        return "HTTPEndpoint{" +
                "path='" + path + '\'' +
                ", https=" + https +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", pollingIntervalSecs=" + pollingIntervalSecs +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
