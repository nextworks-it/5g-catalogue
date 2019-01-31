package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Volumes {

    private String name;
    private String description;
    private Integer size;
    private String ephemeral;
    private String image;
    @JsonProperty("image-checksum")
    private String imageChecksum;
    @JsonProperty("device-bus")
    private String deviceBus;
    @JsonProperty("device-type")
    private String deviceType;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

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

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(String ephemeral) {
        this.ephemeral = ephemeral;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageChecksum() {
        return imageChecksum;
    }

    public void setImageChecksum(String imageChecksum) {
        this.imageChecksum = imageChecksum;
    }

    public String getDeviceBus() {
        return deviceBus;
    }

    public void setDeviceBus(String deviceBus) {
        this.deviceBus = deviceBus;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
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
        return "Volumes{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", size=" + size +
                ", ephemeral='" + ephemeral + '\'' +
                ", image='" + image + '\'' +
                ", imageChecksum='" + imageChecksum + '\'' +
                ", deviceBus='" + deviceBus + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
