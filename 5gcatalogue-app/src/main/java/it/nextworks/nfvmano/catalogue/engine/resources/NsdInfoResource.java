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
package it.nextworks.nfvmano.catalogue.engine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdUsageStateType;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.*;

/**
 * @author nextworks
 */
@Entity
public class NsdInfoResource {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID nsdId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<UUID> vnfPkgIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<UUID> pnfdInfoIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<UUID> nestedNsdInfoIds = new ArrayList<>();

    private NsdOnboardingStateType nsdOnboardingState;

    private NsdOperationalStateType nsdOperationalState;

    private NsdUsageStateType nsdUsageState;

    private String nsdName;

    private String nsdVersion;

    private String nsdDesigner;

    private UUID nsdInvariantId;

    private ContentType contentType;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<String> nsdFilename = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<String> nsdPkgFilename = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private Map<String, String> userDefinedData = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private Map<String, NotificationResource> acknowledgedOnboardOpConsumers = new HashMap<>();

    private String projectId;

    private boolean isPublished;

    public NsdInfoResource() {
    }

    public NsdInfoResource(UUID nsdId, List<UUID> vnfPkgIds, List<UUID> pnfdInfoIds, List<UUID> nestedNsdInfoIds, NsdOnboardingStateType nsdOnboardingState, NsdOperationalStateType nsdOperationalState, NsdUsageStateType nsdUsageState, String nsdName, String nsdVersion, String nsdDesigner, UUID nsdInvariantId, Map<String, String> userDefinedData) {
        this.nsdId = nsdId;
        this.vnfPkgIds = vnfPkgIds;
        this.pnfdInfoIds = pnfdInfoIds;
        this.nestedNsdInfoIds = nestedNsdInfoIds;
        this.nsdOnboardingState = nsdOnboardingState;
        this.nsdOperationalState = nsdOperationalState;
        this.nsdUsageState = nsdUsageState;
        this.nsdName = nsdName;
        this.nsdVersion = nsdVersion;
        this.nsdDesigner = nsdDesigner;
        this.nsdInvariantId = nsdInvariantId;
        this.userDefinedData = userDefinedData;
    }

    public NsdInfoResource(Map<String, String> userDefinedData) {
        if (userDefinedData != null) this.userDefinedData = userDefinedData;
        nsdOnboardingState = NsdOnboardingStateType.CREATED;
        nsdOperationalState = NsdOperationalStateType.DISABLED;
        nsdUsageState = NsdUsageStateType.NOT_IN_USE;
        contentType = ContentType.UNSPECIFIED;
    }

    /**
     * @return the nsdId
     */
    public UUID getNsdId() {
        return nsdId;
    }

    /**
     * @param nsdId the nsdId to set
     */
    public void setNsdId(UUID nsdId) {
        this.nsdId = nsdId;
    }

    /**
     * @return the vnfPkgIds
     */
    public List<UUID> getVnfPkgIds() {
        return vnfPkgIds;
    }

    /**
     * @param vnfPkgIds the vnfPkgIds to set
     */
    public void setVnfPkgIds(List<UUID> vnfPkgIds) {
        this.vnfPkgIds = vnfPkgIds;
    }

    /**
     * @return the pnfdInfoIds
     */
    public List<UUID> getPnfdInfoIds() {
        return pnfdInfoIds;
    }

    /**
     * @param pnfdInfoIds the pnfdInfoIds to set
     */
    public void setPnfdInfoIds(List<UUID> pnfdInfoIds) {
        this.pnfdInfoIds = pnfdInfoIds;
    }

    /**
     * @return the nestedNsdInfoIds
     */
    public List<UUID> getNestedNsdInfoIds() {
        return nestedNsdInfoIds;
    }

    /**
     * @param nestedNsdInfoIds the nestedNsdInfoIds to set
     */
    public void setNestedNsdInfoIds(List<UUID> nestedNsdInfoIds) {
        this.nestedNsdInfoIds = nestedNsdInfoIds;
    }

    /**
     * @return the nsdOnboardingState
     */
    public NsdOnboardingStateType getNsdOnboardingState() {
        return nsdOnboardingState;
    }

    /**
     * @param nsdOnboardingState the nsdOnboardingState to set
     */
    public void setNsdOnboardingState(NsdOnboardingStateType nsdOnboardingState) {
        this.nsdOnboardingState = nsdOnboardingState;
    }

    /**
     * @return the nsdOperationalState
     */
    public NsdOperationalStateType getNsdOperationalState() {
        return nsdOperationalState;
    }

    /**
     * @param nsdOperationalState the nsdOperationalState to set
     */
    public void setNsdOperationalState(NsdOperationalStateType nsdOperationalState) {
        this.nsdOperationalState = nsdOperationalState;
    }

    /**
     * @return the nsdUsageState
     */
    public NsdUsageStateType getNsdUsageState() {
        return nsdUsageState;
    }

    /**
     * @param nsdUsageState the nsdUsageState to set
     */
    public void setNsdUsageState(NsdUsageStateType nsdUsageState) {
        this.nsdUsageState = nsdUsageState;
    }

    /**
     * @return the userDefinedData
     */
    public Map<String, String> getUserDefinedData() {
        return userDefinedData;
    }

    /**
     * @param userDefinedData the userDefinedData to set
     */
    public void setUserDefinedData(Map<String, String> userDefinedData) {
        this.userDefinedData = userDefinedData;
    }

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return the nsdName
     */
    public String getNsdName() {
        return nsdName;
    }

    /**
     * @param nsdName the nsdName to set
     */
    public void setNsdName(String nsdName) {
        this.nsdName = nsdName;
    }

    /**
     * @return the nsdVersion
     */
    public String getNsdVersion() {
        return nsdVersion;
    }

    /**
     * @param nsdVersion the nsdVersion to set
     */
    public void setNsdVersion(String nsdVersion) {
        this.nsdVersion = nsdVersion;
    }

    /**
     * @return the nsdDesigner
     */
    public String getNsdDesigner() {
        return nsdDesigner;
    }

    /**
     * @param nsdDesigner the nsdDesigner to set
     */
    public void setNsdDesigner(String nsdDesigner) {
        this.nsdDesigner = nsdDesigner;
    }

    /**
     * @return the nsdInvariantId
     */
    public UUID getNsdInvariantId() {
        return nsdInvariantId;
    }

    /**
     * @param nsdInvariantId the nsdInvariantId to set
     */
    public void setNsdInvariantId(UUID nsdInvariantId) {
        this.nsdInvariantId = nsdInvariantId;
    }

    /**
     * @return the contentType
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the nsdFilename
     */
    public List<String> getNsdFilename() {
        return nsdFilename;
    }

    public void setNsdFilename(List<String> nsdFilename) {
        this.nsdFilename = nsdFilename;
    }

    public void addNsdFilename(String filename) {
        this.nsdFilename.add(filename);
    }

    public List<String> getNsdPkgFilename() {
        return nsdPkgFilename;
    }

    public void setNsdPkgFilename(List<String> nsdPkgFilename) {
        this.nsdPkgFilename = nsdPkgFilename;
    }

    public void addNsdPkgFilename(String nsdPkgFilename) {
        this.nsdPkgFilename.add(nsdPkgFilename);
    }

    /**
     * @return the acknowledgedOnboardOpConsumers
     */
    public Map<String, NotificationResource> getAcknowledgedOnboardOpConsumers() {
        return acknowledgedOnboardOpConsumers;
    }

    public void setAcknowledgedOnboardOpConsumers(Map<String, NotificationResource> acknowledgedOnboardOpConsumers) {
        this.acknowledgedOnboardOpConsumers = acknowledgedOnboardOpConsumers;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public void isDeletable() throws NotPermittedOperationException {
        if (nsdOperationalState != NsdOperationalStateType.DISABLED)
            throw new NotPermittedOperationException("NSD info " + this.id + " cannot be deleted because not DISABLED");
        if (nsdUsageState != NsdUsageStateType.NOT_IN_USE)
            throw new NotPermittedOperationException("NSD info " + this.id + " cannot be deleted because IN USE");
    }

}
