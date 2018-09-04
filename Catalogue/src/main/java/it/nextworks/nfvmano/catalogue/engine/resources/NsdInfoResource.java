package it.nextworks.nfvmano.catalogue.engine.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdUsageStateType;
import it.nextworks.nfvmano.catalogue.repos.NsdContentType;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;

@Entity
public class NsdInfoResource {

	@Id
	@GeneratedValue
	private UUID id;
	
	private UUID nsdId; 
	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private List<UUID> vnfPkgIds = new ArrayList<>();
	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private List<UUID> pnfdInfoIds = new ArrayList<>();
	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private List<UUID> nestedNsdInfoIds = new ArrayList<>();
	
	private NsdOnboardingStateType nsdOnboardingState;
	
	private NsdOperationalStateType nsdOperationalState;
	
	private NsdUsageStateType nsdUsageState;
	
	private String nsdName;

	private String nsdVersion;

	private String nsdDesigner;

	private UUID nsdInvariantId;
	
	private NsdContentType nsdContentType;
	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private List<String> nsdFilename = new ArrayList<>();

	
	@ElementCollection
	@Fetch(FetchMode.SELECT)
	private Map<String, String> userDefinedData = new HashMap<>();
	
	
	public NsdInfoResource() { }
	
	public NsdInfoResource(Map<String, String> userDefinedData) {
		if (userDefinedData != null) this.userDefinedData = userDefinedData;
		nsdOnboardingState = NsdOnboardingStateType.CREATED;
		nsdOperationalState = NsdOperationalStateType.DISABLED;
		nsdUsageState = NsdUsageStateType.NOT_IN_USE;
		nsdContentType = NsdContentType.UNSPECIFIED;
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
	 * @return the nsdContentType
	 */
	public NsdContentType getNsdContentType() {
		return nsdContentType;
	}

	/**
	 * @param nsdContentType the nsdContentType to set
	 */
	public void setNsdContentType(NsdContentType nsdContentType) {
		this.nsdContentType = nsdContentType;
	}
	
	

	/**
	 * @return the nsdFilename
	 */
	public List<String> getNsdFilename() {
		return nsdFilename;
	}
	
	public void addNsdFilename(String filename) {
		this.nsdFilename.add(filename);
	}

	public void isDeletable() throws NotPermittedOperationException {
		if (nsdOperationalState != NsdOperationalStateType.DISABLED) throw new NotPermittedOperationException("NSD info " + this.id + " cannot be deleted because not DISABLED");
		if (nsdUsageState != NsdUsageStateType.NOT_IN_USE) throw new NotPermittedOperationException("NSD info " + this.id + " cannot be deleted because IN USE");
	}

}
