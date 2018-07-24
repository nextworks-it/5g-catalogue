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
package it.nextworks.nfvmano.catalogue.repos.nsds.repositories;


import com.fasterxml.jackson.annotation.JsonIgnore;
import it.nextworks.nfvmano.libs.common.enums.OperationalState;
import it.nextworks.nfvmano.libs.common.enums.UsageState;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an internal entity used to keep trace of the status of the NSD
 * and its instantiations
 * 
 * @author nextworks
 *
 */
@Entity
public class InternalNsdInfo {

	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;
	
	private String nsdInfoId;
	private String nsdId;
	private String version;
	private String previousNsdVersionId;
	private OperationalState operationalState;
	private UsageState usageState;
	private boolean deletionPending;
	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private Map<String, String> userDefinedData = new HashMap<>();
	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private List<String> pnfdInfoId = new ArrayList<>();
	
	@ElementCollection(fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@Cascade(org.hibernate.annotations.CascadeType.ALL)
	private List<String> nsInstanceId = new ArrayList<>();		//Active instances
	
	public InternalNsdInfo() {	}
	
	public InternalNsdInfo(String nsdInfoId,
	 String nsdId,
	 String version,
	 String previousNsdVersionId,
	 OperationalState operationalState,
	 UsageState usageState,
	 boolean deletionPending,
	 List<String> pnfdInfoId,
	 Map<String,String> userDefinedData) {
		this.nsdInfoId = nsdInfoId;
		this.nsdId = nsdId;
		this.version = version;
		this.previousNsdVersionId = previousNsdVersionId;
		this.operationalState = operationalState;
		this.usageState = usageState;
		this.deletionPending = deletionPending;
		if (pnfdInfoId != null) this.pnfdInfoId = pnfdInfoId;
		if (userDefinedData != null) this.userDefinedData = userDefinedData;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the nsdInfoId
	 */
	public String getNsdInfoId() {
		return nsdInfoId;
	}

	/**
	 * @return the nsdId
	 */
	public String getNsdId() {
		return nsdId;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the previousNsdVersionId
	 */
	public String getPreviousNsdVersionId() {
		return previousNsdVersionId;
	}

	/**
	 * @return the operationalState
	 */
	public OperationalState getOperationalState() {
		return operationalState;
	}

	/**
	 * @return the usageState
	 */
	public UsageState getUsageState() {
		return usageState;
	}

	/**
	 * @return the deletionPending
	 */
	public boolean isDeletionPending() {
		return deletionPending;
	}

	/**
	 * @return the userDefinedData
	 */
	public Map<String, String> getUserDefinedData() {
		return userDefinedData;
	}

	/**
	 * @return the pnfdInfoId
	 */
	public List<String> getPnfdInfoId() {
		return pnfdInfoId;
	}

	/**
	 * @param previousNsdVersionId the previousNsdVersionId to set
	 */
	public void setPreviousNsdVersionId(String previousNsdVersionId) {
		this.previousNsdVersionId = previousNsdVersionId;
	}

	/**
	 * @param operationalState the operationalState to set
	 */
	public void setOperationalState(OperationalState operationalState) {
		this.operationalState = operationalState;
	}

	/**
	 * @param usageState the usageState to set
	 */
	public void setUsageState(UsageState usageState) {
		this.usageState = usageState;
	}

	/**
	 * @param deletionPending the deletionPending to set
	 */
	public void setDeletionPending(boolean deletionPending) {
		this.deletionPending = deletionPending;
	}
	
	

	/**
	 * @return the nsInstanceId
	 */
	public List<String> getNsInstanceId() {
		return nsInstanceId;
	}

	/**
	 * 
	 * @param update new user defined data to be added or updated in the current ones
	 */
	public void mergeUserDefinedData(Map<String,String> update) {
		for (Map.Entry<String, String> e : update.entrySet()) {
			this.userDefinedData.put(e.getKey(), e.getValue());
		}
	}
	
	/**
	 * 
	 * @return true if the NSD can be used to instantiate a new NS
	 */
	public boolean canBeInstantiated() {
		boolean result = true;
		if (operationalState == OperationalState.DISABLED) result = false;
		if (isDeletionPending()) result = false;
		return result;
	}
	
	/**
	 * 
	 * @param nsId NS instance ID to be added in the list of instances available for this NSD
	 */
	public void addNsInstanceId(String nsId) {
		this.nsInstanceId.add(nsId);
	}
	
	/**
	 * 
	 * @param nsId NS instance ID to be removed from the list of instances available for this NSD 
	 */
	public void removeNsInstanceId(String nsId) {
		nsInstanceId.remove(nsId);
	}

}
