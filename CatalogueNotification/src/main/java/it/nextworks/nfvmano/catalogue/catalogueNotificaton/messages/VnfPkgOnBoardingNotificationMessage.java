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
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VnfPkgOnBoardingNotificationMessage extends CatalogueMessage {

    private final String vnfPkgInfoId;
    private final String vnfdId;
    private final KeyValuePair packagePath;
    private final String project;

    private final List<String> siteOrManoIds = new ArrayList<>();

    @JsonInclude(Include.NON_NULL)
    private String pluginId;

    @JsonCreator
    public VnfPkgOnBoardingNotificationMessage(
            @JsonProperty("vnfPkgInfoId") String vnfPkgInfoId,
            @JsonProperty("vnfdId") String vnfdId,
            @JsonProperty("project") String project,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("notifierId") String pluginId,
            @JsonProperty("packagePath") KeyValuePair packagePath
    ) {
        super(CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.vnfPkgInfoId = vnfPkgInfoId;
        this.vnfdId = vnfdId;
        this.pluginId = pluginId;
        this.packagePath = packagePath;
        this.project = project;
    }

    public VnfPkgOnBoardingNotificationMessage(
            @JsonProperty("vnfPkgInfoId") String vnfPkgInfoId,
            @JsonProperty("vnfdId") String vnfdId,
            @JsonProperty("project") String project,
            @JsonProperty("operationId") UUID operationId,
            @JsonProperty("scope") ScopeType scope,
            @JsonProperty("operationStatus") OperationStatus opStatus,
            @JsonProperty("siteOrManoIds") List<String> siteOrManoIds,
            @JsonProperty("packagePath") KeyValuePair packagePath
    ) {
        super(CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION, operationId, scope, opStatus);
        this.vnfPkgInfoId = vnfPkgInfoId;
        this.vnfdId = vnfdId;
        this.pluginId = null;
        this.packagePath = packagePath;
        this.project = project;
        this.siteOrManoIds.addAll(siteOrManoIds);
    }

    @JsonProperty("vnfPkgInfoId")
    public String getVnfPkgInfoId() {
        return vnfPkgInfoId;
    }

    @JsonProperty("vnfdId")
    public String getVnfdId() {
        return vnfdId;
    }

    @JsonProperty("notifierId")
    public String getPluginId() {
        return pluginId;
    }

    @JsonProperty("packagePath")
    public KeyValuePair getPackagePath() {
        return packagePath;
    }

    @JsonProperty("project")
    public String getProject() {
        return project;
    }

    @JsonProperty("siteOrManoIds")
    public List<String> getSiteOrManoIds() {
        return siteOrManoIds;
    }
}
