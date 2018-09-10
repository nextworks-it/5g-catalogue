package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class NsdCatalog {

    public NsdCatalog() {

    }

    private List<Nsd> nsd;

    @JsonProperty("nsd")
    public List<Nsd> getNsd() {
        return nsd;
    }

    @JsonProperty("nsd")
    public NsdCatalog setNsd(List<Nsd> nsd) {
        this.nsd = nsd;
        return this;
    }

}