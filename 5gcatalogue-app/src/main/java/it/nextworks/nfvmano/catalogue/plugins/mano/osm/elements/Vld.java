/*
 * Copyright 2018 Nextworks s.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Vld {

    private String type;
    private List<VnfdConnectionPointRef> vnfdConnectionPointRef;
    private String name;
    private String id;
    private String shortName;
    private String mgmtNetwork;

    public Vld() {

    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public Vld setType(String type) {
        this.type = type;
        return this;
    }

    @JsonProperty("vnfd-connection-point-ref")
    public List<VnfdConnectionPointRef> getVnfdConnectionPointRef() {
        return vnfdConnectionPointRef;
    }

    @JsonProperty("vnfd-connection-point-ref")
    public Vld setVnfdConnectionPointRef(List<VnfdConnectionPointRef> vnfdConnectionPointRef) {
        this.vnfdConnectionPointRef = vnfdConnectionPointRef;
        return this;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public Vld setName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public Vld setId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("short-name")
    public String getShortName() {
        return shortName;
    }

    @JsonProperty("short-name")
    public Vld setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

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