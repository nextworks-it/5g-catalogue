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
    @JsonProperty("_admin")
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

    public static class AdminObject {
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
    }

    public static class StorageObject {
        @JsonProperty("zipfile")
        private String zipfile;
        @JsonProperty("folder")
        private String folder;
        @JsonProperty("fs")
        private String fs;
        @JsonProperty("descriptor")
        private String descriptor;
        @JsonProperty("pkg-dir")
        private String pkgDir;
        @JsonProperty("path")
        private String path;

        public String getZipfile() {
            return zipfile;
        }

        public void setZipfile(String zipfile) {
            this.zipfile = zipfile;
        }

        public String getFolder() {
            return folder;
        }

        public void setFolder(String folder) {
            this.folder = folder;
        }

        public String getFs() {
            return fs;
        }

        public void setFs(String fs) {
            this.fs = fs;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public void setDescriptor(String descriptor) {
            this.descriptor = descriptor;
        }

        public String getPkgDir() {
            return pkgDir;
        }

        public void setPkgDir(String pkgDir) {
            this.pkgDir = pkgDir;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}