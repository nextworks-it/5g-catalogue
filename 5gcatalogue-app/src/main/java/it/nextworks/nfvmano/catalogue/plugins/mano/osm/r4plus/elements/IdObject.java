package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4plus.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IdObject {

    @JsonProperty("id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "IdObject{" +
                "id='" + id + '\'' +
                '}';
    }
}
