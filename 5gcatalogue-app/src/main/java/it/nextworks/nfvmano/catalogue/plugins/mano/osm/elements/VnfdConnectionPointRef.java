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

public class VnfdConnectionPointRef {

    private String vnfdIdRef;
    private String vnfdConnectionPointRef;
    private int memberVnfIndexRef;

    public VnfdConnectionPointRef() {

    }

    @JsonProperty("vnfd-id-ref")
    public String getVnfdIdRef() {
        return vnfdIdRef;
    }

    @JsonProperty("vnfd-id-ref")
    public VnfdConnectionPointRef setVnfdIdRef(String vnfdIdRef) {
        this.vnfdIdRef = vnfdIdRef;
        return this;
    }

    @JsonProperty("vnfd-connection-point-ref")
    public String getVnfdConnectionPointRef() {
        return vnfdConnectionPointRef;
    }

    @JsonProperty("vnfd-connection-point-ref")
    public VnfdConnectionPointRef setVnfdConnectionPointRef(String vnfdConnectionPointRef) {
        this.vnfdConnectionPointRef = vnfdConnectionPointRef;
        return this;
    }

    @JsonProperty("member-vnf-index-ref")
    public int getMemberVnfIndexRef() {
        return memberVnfIndexRef;
    }

    @JsonProperty("member-vnf-index-ref")
    public VnfdConnectionPointRef setMemberVnfIndexRef(int memberVnfIndexRef) {
        this.memberVnfIndexRef = memberVnfIndexRef;
        return this;
    }

}