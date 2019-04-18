package it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Catalogue {

    @Id
    @GeneratedValue
    private Long id;

    private String catalogueId;

    private String ipAddress;

    private String port;

    public Catalogue() {
    }

    public Catalogue(String catalogueId, String ipAddress, String port) {

        this.catalogueId = catalogueId;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public Long getId() {
        return id;
    }

    @JsonProperty("catalogueId")
    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    @JsonProperty("ipAddress")
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @JsonProperty("port")
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
