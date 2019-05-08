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
package it.nextworks.nfvmano.catalogue.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NsdOnBoardingNotificationMessage extends CatalogueMessage {

    private final String nsdInfoId;
    private final String nsdId;

    @JsonInclude(Include.NON_NULL)
    private String pluginId;

    @JsonInclude(Include.NON_EMPTY)
    private List<String> includedVnfds = new ArrayList<>();

    @JsonInclude(Include.NON_EMPTY)
    private List<String> includedPnfds = new ArrayList<>();

    @JsonCreator
    public NsdOnBoardingNotificationMessage(
            @JsonProperty("nsdInfoId") String nsdInfoId,
            @JsonProperty("nsdId") String nsdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("notifierId") String pluginId
    ) {
        super(CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.nsdInfoId = nsdInfoId;
        this.nsdId = nsdId;
        this.pluginId = pluginId;
    }

    public NsdOnBoardingNotificationMessage(
            @JsonProperty("nsdInfoId") String nsdInfoId,
            @JsonProperty("nsdId") String nsdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus
    ) {
        super(CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.nsdInfoId = nsdInfoId;
        this.nsdId = nsdId;
        this.pluginId = null;
    }

    @JsonProperty("nsdInfoId")
    public String getNsdInfoId() {
        return nsdInfoId;
    }

    @JsonProperty("nsdId")
    public String getNsdId() {
        return nsdId;
    }

    @JsonProperty("notifierId")
    public String getPluginId() {
        return pluginId;
    }

    public List<String> getIncludedVnfds() {
        return includedVnfds;
    }

    public void setIncludedVnfds(List<String> includedVnfds) {
        this.includedVnfds = includedVnfds;
    }

    public List<String> getIncludedPnfds() {
        return includedPnfds;
    }

    public void setIncludedPnfds(List<String> includedPnfds) {
        this.includedPnfds = includedPnfds;
    }
}
