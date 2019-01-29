package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class MonitoringParam {

    private String id;
    private String name;
    private String description;
    @JsonProperty("aggregation-type")
    private String aggregationType;
    @JsonProperty("vdu-monitoring-param")
    private VduMonitoringParam vduMonitoringParam;
    @JsonProperty("vnf-metric")
    private VnfMetric vnfMetric;
    @JsonProperty("vdu-metric")
    private VduMetric vduMetric;
    @JsonProperty("http-endpoint-ref")
    private String httpEndpointRef;
    @JsonProperty("json-query-method")
    private String jsonQueryMethos;
    @JsonProperty("json-query-params")
    private JsonQueryParams jsonQueryParams;
    @JsonProperty("value-type")
    private String valueType;
    @JsonProperty("numeric-constraints")
    private NumericConstraints numericConstraints;
    @JsonProperty("text-constraints")
    private TextConstraints textConstraints;
    @JsonProperty("value-integer")
    private Integer valueInteger;
    @JsonProperty("value-string")
    private String valueString;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public VduMonitoringParam getVduMonitoringParam() {
        return vduMonitoringParam;
    }

    public void setVduMonitoringParam(VduMonitoringParam vduMonitoringParam) {
        this.vduMonitoringParam = vduMonitoringParam;
    }

    public VnfMetric getVnfMetric() {
        return vnfMetric;
    }

    public void setVnfMetric(VnfMetric vnfMetric) {
        this.vnfMetric = vnfMetric;
    }

    public VduMetric getVduMetric() {
        return vduMetric;
    }

    public void setVduMetric(VduMetric vduMetric) {
        this.vduMetric = vduMetric;
    }

    public String getHttpEndpointRef() {
        return httpEndpointRef;
    }

    public void setHttpEndpointRef(String httpEndpointRef) {
        this.httpEndpointRef = httpEndpointRef;
    }

    public String getJsonQueryMethos() {
        return jsonQueryMethos;
    }

    public void setJsonQueryMethos(String jsonQueryMethos) {
        this.jsonQueryMethos = jsonQueryMethos;
    }

    public JsonQueryParams getJsonQueryParams() {
        return jsonQueryParams;
    }

    public void setJsonQueryParams(JsonQueryParams jsonQueryParams) {
        this.jsonQueryParams = jsonQueryParams;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public NumericConstraints getNumericConstraints() {
        return numericConstraints;
    }

    public void setNumericConstraints(NumericConstraints numericConstraints) {
        this.numericConstraints = numericConstraints;
    }

    public TextConstraints getTextConstraints() {
        return textConstraints;
    }

    public void setTextConstraints(TextConstraints textConstraints) {
        this.textConstraints = textConstraints;
    }

    public Integer getValueInteger() {
        return valueInteger;
    }

    public void setValueInteger(Integer valueInteger) {
        this.valueInteger = valueInteger;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
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
        return "MonitoringParam{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", aggregationType='" + aggregationType + '\'' +
                ", vduMonitoringParam=" + vduMonitoringParam +
                ", vnfMetric=" + vnfMetric +
                ", vduMetric=" + vduMetric +
                ", httpEndpointRef='" + httpEndpointRef + '\'' +
                ", jsonQueryMethos='" + jsonQueryMethos + '\'' +
                ", jsonQueryParams=" + jsonQueryParams +
                ", valueType='" + valueType + '\'' +
                ", numericConstraints=" + numericConstraints +
                ", textConstraints=" + textConstraints +
                ", valueInteger=" + valueInteger +
                ", valueString='" + valueString + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
