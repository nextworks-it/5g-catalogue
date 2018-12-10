package it.nextworks.nfvmano.catalogue.engine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdUsageStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.*;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
public class VnfPkgInfoResource {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID vnfdId;
    private String vnfProvider;
    private String vnfProductName;
    private String vnfSoftwareVersion;
    private String vnfdVersion;
    private String checksum;
    private PackageOnboardingStateType onboardingState;
    private PackageOperationalStateType operationalState;
    private PackageUsageStateType usageState;

    @ElementCollection(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private Map<String, String> userDefinedData = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch= FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private Map<String, NotificationResource> acknowledgedOnboardOpConsumers = new HashMap<>();

    private ContentType contentType;

    private String vnfPkgFilename;

    public VnfPkgInfoResource() {
    }

    public VnfPkgInfoResource(Map<String, String> userDefinedData) {
        if (userDefinedData != null) this.userDefinedData = userDefinedData;
        onboardingState = PackageOnboardingStateType.CREATED;
        operationalState = PackageOperationalStateType.DISABLED;
        usageState = PackageUsageStateType.NOT_IN_USE;
        contentType = ContentType.UNSPECIFIED;
    }

    public VnfPkgInfoResource(UUID vnfdId, String vnfProvider, String vnfProductName, String vnfSoftwareVersion, String vnfdVersion, PackageOnboardingStateType onboardingState, PackageOperationalStateType operationalState, PackageUsageStateType usageState, KeyValuePairs userDefinedData) {
        this.vnfdId = vnfdId;
        this.vnfProvider = vnfProvider;
        this.vnfProductName = vnfProductName;
        this.vnfSoftwareVersion = vnfSoftwareVersion;
        this.vnfdVersion = vnfdVersion;
        this.onboardingState = onboardingState;
        this.operationalState = operationalState;
        this.usageState = usageState;
        this.userDefinedData = userDefinedData;
    }

    public VnfPkgInfoResource(UUID vnfdId, String vnfProvider, String vnfProductName, String vnfSoftwareVersion, String vnfdVersion, String checksum, PackageOnboardingStateType onboardingState, PackageOperationalStateType operationalState, PackageUsageStateType usageState, KeyValuePairs userDefinedData, Map<String, NotificationResource> acknowledgedOnboardOpConsumers) {
        this.vnfdId = vnfdId;
        this.vnfProvider = vnfProvider;
        this.vnfProductName = vnfProductName;
        this.vnfSoftwareVersion = vnfSoftwareVersion;
        this.vnfdVersion = vnfdVersion;
        this.checksum = checksum;
        this.onboardingState = onboardingState;
        this.operationalState = operationalState;
        this.usageState = usageState;
        this.userDefinedData = userDefinedData;
        this.acknowledgedOnboardOpConsumers = acknowledgedOnboardOpConsumers;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVnfdId() {
        return vnfdId;
    }

    public String getVnfProvider() {
        return vnfProvider;
    }

    public String getVnfProductName() {
        return vnfProductName;
    }

    public String getVnfSoftwareVersion() {
        return vnfSoftwareVersion;
    }

    public String getVnfdVersion() {
        return vnfdVersion;
    }

    public String getChecksum() {
        return checksum;
    }

    public PackageOnboardingStateType getOnboardingState() {
        return onboardingState;
    }

    public PackageOperationalStateType getOperationalState() {
        return operationalState;
    }

    public PackageUsageStateType getUsageState() {
        return usageState;
    }

    public Map<String, String> getUserDefinedData() {
        return userDefinedData;
    }

    public Map<String, NotificationResource> getAcknowledgedOnboardOpConsumers() {
        return acknowledgedOnboardOpConsumers;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getVnfPkgFilename() {
        return vnfPkgFilename;
    }

    public void setVnfdId(UUID vnfdId) {
        this.vnfdId = vnfdId;
    }

    public void setVnfProvider(String vnfProvider) {
        this.vnfProvider = vnfProvider;
    }

    public void setVnfProductName(String vnfProductName) {
        this.vnfProductName = vnfProductName;
    }

    public void setVnfSoftwareVersion(String vnfSoftwareVersion) {
        this.vnfSoftwareVersion = vnfSoftwareVersion;
    }

    public void setVnfdVersion(String vnfdVersion) {
        this.vnfdVersion = vnfdVersion;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setOnboardingState(PackageOnboardingStateType onboardingState) {
        this.onboardingState = onboardingState;
    }

    public void setOperationalState(PackageOperationalStateType operationalState) {
        this.operationalState = operationalState;
    }

    public void setUsageState(PackageUsageStateType usageState) {
        this.usageState = usageState;
    }

    public void setUserDefinedData(Map<String, String> userDefinedData) {
        this.userDefinedData = userDefinedData;
    }

    public void setAcknowledgedOnboardOpConsumers(Map<String, NotificationResource> acknowledgedOnboardOpConsumers) {
        this.acknowledgedOnboardOpConsumers = acknowledgedOnboardOpConsumers;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public void setVnfPkgFilename(String vnfPkgFilename) {
        this.vnfPkgFilename = vnfPkgFilename;
    }
}
