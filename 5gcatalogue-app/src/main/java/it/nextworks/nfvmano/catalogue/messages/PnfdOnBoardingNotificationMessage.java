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
import it.nextworks.nfvmano.catalogue.messages.elements.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.messages.elements.ScopeType;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

import java.util.UUID;

public class PnfdOnBoardingNotificationMessage extends CatalogueMessage {

    private final String pnfdInfoId;
    private final String pnfdId;

    @JsonInclude(Include.NON_NULL)
    private String pluginId;

    @JsonCreator
    public PnfdOnBoardingNotificationMessage(
            @JsonProperty("pnfdInfoId") String pnfdInfoId,
            @JsonProperty("pnfdId") String pnfdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("notifierId") String pluginId
    ) {
        super(CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.pnfdInfoId = pnfdInfoId;
        this.pnfdId = pnfdId;
        this.pluginId = pluginId;
    }

    public PnfdOnBoardingNotificationMessage(
            @JsonProperty("pnfdInfoId") String pnfdInfoId,
            @JsonProperty("pnfdId") String pnfdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus
    ) {
        super(CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.pnfdInfoId = pnfdInfoId;
        this.pnfdId = pnfdId;
        this.pluginId = null;
    }

    @JsonProperty("pnfdInfoId")
    public String getPnfdInfoId() {
        return pnfdInfoId;
    }

    @JsonProperty("pnfdId")
    public String getPnfdId() {
        return pnfdId;
    }

    @JsonProperty("notifierId")
    public String getPluginId() {
        return pluginId;
    }
}
