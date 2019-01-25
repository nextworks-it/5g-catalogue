package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScalingPolicy {

    private String name;
    private boolean enabled;
    @JsonProperty("scaling-type")
    private String scalingType;
    @JsonProperty("scale-in-operation-type")
    private String scaleInOperationType;
    @JsonProperty("scale-out-operation-type")
    private String scaleOutOperationType;
    @JsonProperty("threshold-time")
    private Integer thresholdTime;
    @JsonProperty("cooldown-time")
    private Integer cooldownTime;
    @JsonProperty("scaling-criteria")
    private List<ScalingCriteria> scalingCriteria;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getScalingType() {
        return scalingType;
    }

    public void setScalingType(String scalingType) {
        this.scalingType = scalingType;
    }

    public String getScaleInOperationType() {
        return scaleInOperationType;
    }

    public void setScaleInOperationType(String scaleInOperationType) {
        this.scaleInOperationType = scaleInOperationType;
    }

    public String getScaleOutOperationType() {
        return scaleOutOperationType;
    }

    public void setScaleOutOperationType(String scaleOutOperationType) {
        this.scaleOutOperationType = scaleOutOperationType;
    }

    public Integer getThresholdTime() {
        return thresholdTime;
    }

    public void setThresholdTime(Integer thresholdTime) {
        this.thresholdTime = thresholdTime;
    }

    public Integer getCooldownTime() {
        return cooldownTime;
    }

    public void setCooldownTime(Integer cooldownTime) {
        this.cooldownTime = cooldownTime;
    }

    public List<ScalingCriteria> getScalingCriteria() {
        return scalingCriteria;
    }

    public void setScalingCriteria(List<ScalingCriteria> scalingCriteria) {
        this.scalingCriteria = scalingCriteria;
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
        return "ScalingPolicy{" +
                "name='" + name + '\'' +
                ", enabled=" + enabled +
                ", scalingType='" + scalingType + '\'' +
                ", scaleInOperationType='" + scaleInOperationType + '\'' +
                ", scaleOutOperationType='" + scaleOutOperationType + '\'' +
                ", thresholdTime=" + thresholdTime +
                ", cooldownTime=" + cooldownTime +
                ", scalingCriteria=" + scalingCriteria +
                ", otherProperties=" + otherProperties +
                '}';
    }
}