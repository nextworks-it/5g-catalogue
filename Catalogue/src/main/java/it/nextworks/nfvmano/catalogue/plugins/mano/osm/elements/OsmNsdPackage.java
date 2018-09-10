package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class OsmNsdPackage {

    public OsmNsdPackage() {

    }

    private NsdCatalog nsdCatalog;

    @JsonProperty("nsd:nsd-catalog")
    public NsdCatalog getNsdCatalog() {
        return nsdCatalog;
    }

    @JsonProperty("nsd:nsd-catalog")
    public OsmNsdPackage setNsdCatalog(NsdCatalog nsdCatalog) {
        this.nsdCatalog = nsdCatalog;
        return this;
    }

}