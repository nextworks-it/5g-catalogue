package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class GuestEpa {

    @JsonProperty("trusted-execution")
    private boolean trustedExecution;
    @JsonProperty("mempage-size")
    private String mempageSize;
    @JsonProperty("cpu-pinning-policy")
    private String cpuPinningPolicy;
    @JsonProperty("cpu-thread-pinning-policy")
    private String cpuThreadPinningPolicy;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public boolean isTrustedExecution() {
        return trustedExecution;
    }

    public void setTrustedExecution(boolean trustedExecution) {
        this.trustedExecution = trustedExecution;
    }

    public String getMempageSize() {
        return mempageSize;
    }

    public void setMempageSize(String mempageSize) {
        this.mempageSize = mempageSize;
    }

    public String getCpuPinningPolicy() {
        return cpuPinningPolicy;
    }

    public void setCpuPinningPolicy(String cpuPinningPolicy) {
        this.cpuPinningPolicy = cpuPinningPolicy;
    }

    public String getCpuThreadPinningPolicy() {
        return cpuThreadPinningPolicy;
    }

    public void setCpuThreadPinningPolicy(String cpuThreadPinningPolicy) {
        this.cpuThreadPinningPolicy = cpuThreadPinningPolicy;
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
        return "GuestEpa{" +
                "trustedExecution=" + trustedExecution +
                ", mempageSize='" + mempageSize + '\'' +
                ", cpuPinningPolicy='" + cpuPinningPolicy + '\'' +
                ", cpuThreadPinningPolicy='" + cpuThreadPinningPolicy + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}