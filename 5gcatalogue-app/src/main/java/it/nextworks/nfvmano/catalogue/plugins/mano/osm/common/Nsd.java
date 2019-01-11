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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Nsd {

    private String description;
    private String logo;
    private String version;
    private List<Vld> vld;
    private String name;
    private String vendor;
    private String id;
    private List<ConstituentVnfd> constituentVnfd;
    private String shortName;

    public Nsd() {

    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public Nsd setDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonProperty("logo")
    public String getLogo() {
        return logo;
    }

    @JsonProperty("logo")
    public Nsd setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public Nsd setVersion(String version) {
        this.version = version;
        return this;
    }

    @JsonProperty("vld")
    public List<Vld> getVld() {
        return vld;
    }

    @JsonProperty("vld")
    public Nsd setVld(List<Vld> vld) {
        this.vld = vld;
        return this;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public Nsd setName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("vendor")
    public String getVendor() {
        return vendor;
    }

    @JsonProperty("vendor")
    public Nsd setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public Nsd setId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("constituent-vnfd")
    public List<ConstituentVnfd> getConstituentVnfd() {
        return constituentVnfd;
    }

    @JsonProperty("constituent-vnfd")
    public Nsd setConstituentVnfd(List<ConstituentVnfd> constituentVnfd) {
        this.constituentVnfd = constituentVnfd;
        return this;
    }

    @JsonProperty("short-name")
    public String getShortName() {
        return shortName;
    }

    @JsonProperty("short-name")
    public Nsd setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

}