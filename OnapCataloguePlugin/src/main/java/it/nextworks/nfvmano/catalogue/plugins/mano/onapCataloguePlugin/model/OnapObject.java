package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model;

import javax.persistence.Entity;

@Entity
public class OnapObject {

    private String id;
    private OnapObjectType type;
    private Long epoch;
    private String onapId;
}
