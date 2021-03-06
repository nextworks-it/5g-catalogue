package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class OnapObject {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String descriptorId;
    private String version;
    private OnapObjectType type;
    private String catalogueId;
    private Long epoch;
    private String onapId;
    private String path;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescriptorId() {
        return descriptorId;
    }

    public void setDescriptorId(String descriptorId) {
        this.descriptorId = descriptorId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public OnapObjectType getType() {
        return type;
    }

    public void setType(OnapObjectType type) {
        this.type = type;
    }

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    public String getOnapId() {
        return onapId;
    }

    public void setOnapId(String onapId) {
        this.onapId = onapId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }
}
