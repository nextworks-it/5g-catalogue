package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class Vld {

    public Vld() {

    }

    private String type;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public Vld setType(String type) {
        this.type = type;
        return this;
    }

    private List<VnfdConnectionPointRef> vnfdConnectionPointRef;

    @JsonProperty("vnfd-connection-point-ref")
    public List<VnfdConnectionPointRef> getVnfdConnectionPointRef() {
        return vnfdConnectionPointRef;
    }

    @JsonProperty("vnfd-connection-point-ref")
    public Vld setVnfdConnectionPointRef(List<VnfdConnectionPointRef> vnfdConnectionPointRef) {
        this.vnfdConnectionPointRef = vnfdConnectionPointRef;
        return this;
    }

    private String name;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public Vld setName(String name) {
        this.name = name;
        return this;
    }

    private String id;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public Vld setId(String id) {
        this.id = id;
        return this;
    }

    private String shortName;

    @JsonProperty("short-name")
    public String getShortName() {
        return shortName;
    }

    @JsonProperty("short-name")
    public Vld setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    private String mgmtNetwork;

    @JsonProperty("mgmt-network")
    public String getMgmtNetwork() {
        return mgmtNetwork;
    }

    @JsonProperty("mgmt-network")
    public Vld setMgmtNetwork(String mgmtNetwork) {
        this.mgmtNetwork = mgmtNetwork;
        return this;
    }

}