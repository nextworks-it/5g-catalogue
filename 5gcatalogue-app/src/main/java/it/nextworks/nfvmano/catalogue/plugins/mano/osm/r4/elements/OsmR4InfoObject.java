package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4.elements;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OsmR4InfoObject {

    @JsonProperty("_admin")
    private AdminObject admin;
    @JsonProperty("vendor")
    private String vendor;
    @JsonProperty("name")
    private String name;
    @JsonProperty("_id")
    private String id;
    @JsonProperty("id")
    private String descriptorId;
    @JsonProperty("version")
    private String version;
    @JsonProperty("description")
    private String description;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public AdminObject getAdmin() {
        return admin;
    }

    public void setAdmin(AdminObject admin) {
        this.admin = admin;
    }

    public String getVendor() { return vendor; }

    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getDescriptorId() { return descriptorId; }

    public void setDescriptorId(String descriptorId) { this.descriptorId = descriptorId; }

    public String getVersion() { return version; }

    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

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
        return "OsmR4InfoObject{" +
                "admin=" + admin +
                ", vendor='" + vendor + '\'' +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", descriptorId='" + descriptorId + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}