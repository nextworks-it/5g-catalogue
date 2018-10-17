package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class Nsd {

    public Nsd() {

    }

    private String description;

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public Nsd setDescription(String description) {
        this.description = description;
        return this;
    }

    private String logo;

    @JsonProperty("logo")
    public String getLogo() {
        return logo;
    }

    @JsonProperty("logo")
    public Nsd setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    private String version;

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public Nsd setVersion(String version) {
        this.version = version;
        return this;
    }

    private List<Vld> vld;

    @JsonProperty("vld")
    public List<Vld> getVld() {
        return vld;
    }

    @JsonProperty("vld")
    public Nsd setVld(List<Vld> vld) {
        this.vld = vld;
        return this;
    }

    private String name;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public Nsd setName(String name) {
        this.name = name;
        return this;
    }

    private String vendor;

    @JsonProperty("vendor")
    public String getVendor() {
        return vendor;
    }

    @JsonProperty("vendor")
    public Nsd setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    private String id;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public Nsd setId(String id) {
        this.id = id;
        return this;
    }

    private List<ConstituentVnfd> constituentVnfd;

    @JsonProperty("constituent-vnfd")
    public List<ConstituentVnfd> getConstituentVnfd() {
        return constituentVnfd;
    }

    @JsonProperty("constituent-vnfd")
    public Nsd setConstituentVnfd(List<ConstituentVnfd> constituentVnfd) {
        this.constituentVnfd = constituentVnfd;
        return this;
    }

    private String shortName;

    @JsonProperty("short-name")
    public String getShortName() {
        return shortName;
    }

    @JsonProperty("short-name")
    public Nsd setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

}