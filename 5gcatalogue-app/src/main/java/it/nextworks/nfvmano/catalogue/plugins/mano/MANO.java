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
package it.nextworks.nfvmano.catalogue.plugins.mano;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.nextworks.nfvmano.catalogue.plugins.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "manoType", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = OSMMano.class, name = "OSMR3"),
        @JsonSubTypes.Type(value = OSMMano.class, name = "OSMR4"),
        @JsonSubTypes.Type(value = OSMMano.class, name = "OSMR5"),
        @JsonSubTypes.Type(value = DummyMano.class, name = "DUMMY")})
public abstract class MANO {

    @Id
    @GeneratedValue
    private Long id;

    private String manoId;
    private MANOType manoType;
    private PluginOperationalState pluginOperationalState;

    public MANO() {
        // JPA only
    }

    public MANO(String manoId) {
        this.manoId = manoId;
    }

    public MANO(String manoId, MANOType manoType) {
        this.manoId = manoId;
        this.manoType = manoType;
    }

    public MANO(String manoId, PluginOperationalState pluginOperationalState) {
        this.manoId = manoId;
        this.pluginOperationalState = pluginOperationalState;
    }

    public MANO(String manoId, MANOType manoType, PluginOperationalState pluginOperationalState) {
        this.manoId = manoId;
        this.manoType = manoType;
        this.pluginOperationalState = pluginOperationalState;
    }

    public Long getId() {
        return id;
    }

    @JsonProperty("manoId")
    public String getManoId() {
        return manoId;
    }

    @JsonProperty("manoType")
    public MANOType getManoType() {
        return manoType;
    }

    @JsonProperty("pluginOperationalState")
    public PluginOperationalState getPluginOperationalState() {
        return pluginOperationalState;
    }

    public void setPluginOperationalState(PluginOperationalState pluginOperationalState) {
        this.pluginOperationalState = pluginOperationalState;
    }
}
