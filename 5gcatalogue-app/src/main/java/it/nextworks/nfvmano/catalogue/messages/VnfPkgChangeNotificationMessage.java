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
import it.nextworks.nfvmano.catalogue.messages.elements.PackageChangeType;
import it.nextworks.nfvmano.catalogue.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PackageOperationalStateType;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

import java.util.UUID;

public class VnfPkgChangeNotificationMessage extends CatalogueMessage {


    private final String vnfPkgInfoId;
    private final String vnfdId;
    private final PackageOperationalStateType operationalState;
    private final PackageChangeType changeType;

    @JsonInclude(Include.NON_NULL)
    private String pluginId;

    @JsonCreator
    public VnfPkgChangeNotificationMessage(
            @JsonProperty("vnfPkgInfoId") String vnfPkgInfoId,
            @JsonProperty("vnfdId") String vnfdId,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("operationalState") PackageOperationalStateType operationalState,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("changeType") PackageChangeType changeType,
            @JsonProperty("notifierId") String pluginId
    ) {
        super(CatalogueMessageType.VNFPKG_CHANGE_NOTIFICATION, operationId, scope, opStatus);
        this.vnfPkgInfoId = vnfPkgInfoId;
        this.vnfdId = vnfdId;
        this.operationalState = operationalState;
        this.changeType = changeType;
        this.pluginId = pluginId;
    }

    @JsonProperty("vnfPkgInfoId")
    public String getVnfPkgInfoId() {
        return vnfPkgInfoId;
    }

    @JsonProperty("vnfdId")
    public String getVnfdId() {
        return vnfdId;
    }

    @JsonProperty("operationalState")
    public PackageOperationalStateType getOperationalState() {
        return operationalState;
    }

    @JsonProperty("changeType")
    public PackageChangeType getChangeType() {
        return changeType;
    }

    @JsonProperty("notifierId")
    public String getPluginId() {
        return pluginId;
    }
}
