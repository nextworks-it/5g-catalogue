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

public class NsdCatalog {

    private List<Nsd> nsd;

    public NsdCatalog() {

    }

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