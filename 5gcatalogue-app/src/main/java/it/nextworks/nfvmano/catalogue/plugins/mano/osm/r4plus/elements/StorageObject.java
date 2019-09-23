package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4plus.elements;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class StorageObject {

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

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

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
        return "StorageObject{" +
                "zipfile='" + zipfile + '\'' +
                ", folder='" + folder + '\'' +
                ", fs='" + fs + '\'' +
                ", descriptor='" + descriptor + '\'' +
                ", pkgDir='" + pkgDir + '\'' +
                ", path='" + path + '\'' +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
