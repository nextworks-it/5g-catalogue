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
package it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import javafx.util.Pair;

import java.util.UUID;

public class PnfdOnBoardingNotificationMessage extends CatalogueMessage {

    private final String pnfdInfoId;
    private final String pnfdId;
    private final KeyValuePair packagePath;

    @JsonInclude(Include.NON_NULL)
    private String pluginId;

    @JsonCreator
    public PnfdOnBoardingNotificationMessage(
            @JsonProperty("pnfdInfoId") String pnfdInfoId,
            @JsonProperty("pnfdId") String pnfdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("notifierId") String pluginId,
            @JsonProperty("packagePath") KeyValuePair packagePath
    ) {
        super(CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.pnfdInfoId = pnfdInfoId;
        this.pnfdId = pnfdId;
        this.pluginId = pluginId;
        this.packagePath = packagePath;
    }

    public PnfdOnBoardingNotificationMessage(
            @JsonProperty("pnfdInfoId") String pnfdInfoId,
            @JsonProperty("pnfdId") String pnfdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("packagePath") KeyValuePair packagePath
    ) {
        super(CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.pnfdInfoId = pnfdInfoId;
        this.pnfdId = pnfdId;
        this.pluginId = null;
        this.packagePath = packagePath;
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

    @JsonProperty("packagePath")
    public KeyValuePair getPackagePath() {
        return packagePath;
    }
}
