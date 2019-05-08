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

    private String url;

    public Catalogue() {
    }

    public Catalogue(String catalogueId, String url) {

        this.catalogueId = catalogueId;
        this.url = url;
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

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
