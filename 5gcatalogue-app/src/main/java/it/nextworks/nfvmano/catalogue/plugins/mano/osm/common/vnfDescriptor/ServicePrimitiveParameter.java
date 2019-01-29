package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ServicePrimitiveParameter {

    private String name;
    @JsonProperty("data-type")
    private String dataType;
    private boolean mandatory;
    @JsonProperty("default-value")
    private String defaultValue;
    @JsonProperty("parameter-pool")
    private String parameterPool;
    @JsonProperty("read-only")
    private boolean readOnly ;
    private boolean hidden;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getParameterPool() {
        return parameterPool;
    }

    public void setParameterPool(String parameterPool) {
        this.parameterPool = parameterPool;
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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
        return "ServicePrimitiveParameter{" +
                "name='" + name + '\'' +
                ", dataType='" + dataType + '\'' +
                ", mandatory=" + mandatory +
                ", defaultValue='" + defaultValue + '\'' +
                ", parameterPool='" + parameterPool + '\'' +
                ", readOnly=" + readOnly +
                ", hidden=" + hidden +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
