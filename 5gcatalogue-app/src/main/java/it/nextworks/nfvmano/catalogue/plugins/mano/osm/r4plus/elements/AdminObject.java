package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4plus.elements;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminObject {

    @JsonProperty("projects_read")
    private ArrayList<String> projectRead;
    @JsonProperty("projects_write")
    private ArrayList<String> projectWrite;
    @JsonProperty("modified")
    private String modified;
    @JsonProperty("operationalState")
    private String operationalState;
    @JsonProperty("onboardingState")
    private String onboardingState;
    @JsonProperty("usageSate")
    private String usageState;
    @JsonProperty("storage")
    private StorageObject storage;
    @JsonProperty("created")
    private String created;
    @JsonProperty("userDefinedData")
    private Map<String, String> userDefinedData;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public ArrayList<String> getProjectRead() {
        return projectRead;
    }

    public void setProjectRead(ArrayList<String> projectRead) {
        this.projectRead = projectRead;
    }

    public ArrayList<String> getProjectWrite() {
        return projectWrite;
    }

    public void setProjectWrite(ArrayList<String> projectWrite) {
        this.projectWrite = projectWrite;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getOperationalState() {
        return operationalState;
    }

    public void setOperationalState(String operationalState) {
        this.operationalState = operationalState;
    }

    public String getOnboardingState() {
        return onboardingState;
    }

    public void setOnboardingState(String onboardingState) {
        this.onboardingState = onboardingState;
    }

    public String getUsageState() {
        return usageState;
    }

    public void setUsageState(String usageState) {
        this.usageState = usageState;
    }

    public StorageObject getStorage() {
        return storage;
    }

    public void setStorage(StorageObject storage) {
        this.storage = storage;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public Map<String, String> getUserDefinedData() {
        return userDefinedData;
    }

    public void setUserDefinedData(Map<String, String> userDefinedData) {
        this.userDefinedData = userDefinedData;
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
        return "AdminObject{" +
                "projectRead=" + projectRead +
                ", projectWrite=" + projectWrite +
                ", modified='" + modified + '\'' +
                ", operationalState='" + operationalState + '\'' +
                ", onboardingState='" + onboardingState + '\'' +
                ", usageState='" + usageState + '\'' +
                ", storage=" + storage +
                ", created='" + created + '\'' +
                ", userDefinedData=" + userDefinedData +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
