package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PackageOnboardingStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PackageOperationalStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PackageUsageStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPackageArtifactInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPackageSoftwareImageInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * VnfPkgInfo
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-10-05T11:50:31.473+02:00")

public class VnfPkgInfo   {
  @JsonProperty("id")
  private UUID id = null;

  @JsonProperty("vnfdId")
  private UUID vnfdId = null;

  @JsonProperty("vnfProvider")
  private String vnfProvider = null;

  @JsonProperty("vnfProductName")
  private String vnfProductName = null;

  @JsonProperty("vnfSoftwareVersion")
  private String vnfSoftwareVersion = null;

  @JsonProperty("vnfdVersion")
  private String vnfdVersion = null;

  @JsonProperty("checksum")
  private String checksum = null;

  @JsonProperty("softwareImages")
  @Valid
  private List<VnfPackageSoftwareImageInfo> softwareImages = null;

  @JsonProperty("additionalArtifacts")
  @Valid
  private List<VnfPackageArtifactInfo> additionalArtifacts = null;

  @JsonProperty("onboardingState")
  private PackageOnboardingStateType onboardingState = null;

  @JsonProperty("operationalState")
  private PackageOperationalStateType operationalState = null;

  @JsonProperty("usageState")
  private PackageUsageStateType usageState = null;

  @JsonProperty("userDefinedData")
  private KeyValuePairs userDefinedData = null;

  @JsonProperty("_links")
  private String links = null;

  @JsonProperty("self")
  private String self = null;

  @JsonProperty("vnfd")
  private String vnfd = null;

  @JsonProperty("packageContent")
  private String packageContent = null;

  public VnfPkgInfo id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  **/
  @ApiModelProperty(value = "")

  @Valid

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public VnfPkgInfo vnfdId(UUID vnfdId) {
    this.vnfdId = vnfdId;
    return this;
  }

  /**
   * Get vnfdId
   * @return vnfdId
  **/
  @ApiModelProperty(value = "")

  @Valid

  public UUID getVnfdId() {
    return vnfdId;
  }

  public void setVnfdId(UUID vnfdId) {
    this.vnfdId = vnfdId;
  }

  public VnfPkgInfo vnfProvider(String vnfProvider) {
    this.vnfProvider = vnfProvider;
    return this;
  }

  /**
   * Get vnfProvider
   * @return vnfProvider
  **/
  @ApiModelProperty(value = "")


  public String getVnfProvider() {
    return vnfProvider;
  }

  public void setVnfProvider(String vnfProvider) {
    this.vnfProvider = vnfProvider;
  }

  public VnfPkgInfo vnfProductName(String vnfProductName) {
    this.vnfProductName = vnfProductName;
    return this;
  }

  /**
   * Get vnfProductName
   * @return vnfProductName
  **/
  @ApiModelProperty(value = "")


  public String getVnfProductName() {
    return vnfProductName;
  }

  public void setVnfProductName(String vnfProductName) {
    this.vnfProductName = vnfProductName;
  }

  public VnfPkgInfo vnfSoftwareVersion(String vnfSoftwareVersion) {
    this.vnfSoftwareVersion = vnfSoftwareVersion;
    return this;
  }

  /**
   * Get vnfSoftwareVersion
   * @return vnfSoftwareVersion
  **/
  @ApiModelProperty(value = "")


  public String getVnfSoftwareVersion() {
    return vnfSoftwareVersion;
  }

  public void setVnfSoftwareVersion(String vnfSoftwareVersion) {
    this.vnfSoftwareVersion = vnfSoftwareVersion;
  }

  public VnfPkgInfo vnfdVersion(String vnfdVersion) {
    this.vnfdVersion = vnfdVersion;
    return this;
  }

  /**
   * Get vnfdVersion
   * @return vnfdVersion
  **/
  @ApiModelProperty(value = "")


  public String getVnfdVersion() {
    return vnfdVersion;
  }

  public void setVnfdVersion(String vnfdVersion) {
    this.vnfdVersion = vnfdVersion;
  }

  public VnfPkgInfo checksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  /**
   * Get checksum
   * @return checksum
  **/
  @ApiModelProperty(value = "")


  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public VnfPkgInfo softwareImages(List<VnfPackageSoftwareImageInfo> softwareImages) {
    this.softwareImages = softwareImages;
    return this;
  }

  public VnfPkgInfo addSoftwareImagesItem(VnfPackageSoftwareImageInfo softwareImagesItem) {
    if (this.softwareImages == null) {
      this.softwareImages = new ArrayList<VnfPackageSoftwareImageInfo>();
    }
    this.softwareImages.add(softwareImagesItem);
    return this;
  }

  /**
   * Get softwareImages
   * @return softwareImages
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<VnfPackageSoftwareImageInfo> getSoftwareImages() {
    return softwareImages;
  }

  public void setSoftwareImages(List<VnfPackageSoftwareImageInfo> softwareImages) {
    this.softwareImages = softwareImages;
  }

  public VnfPkgInfo additionalArtifacts(List<VnfPackageArtifactInfo> additionalArtifacts) {
    this.additionalArtifacts = additionalArtifacts;
    return this;
  }

  public VnfPkgInfo addAdditionalArtifactsItem(VnfPackageArtifactInfo additionalArtifactsItem) {
    if (this.additionalArtifacts == null) {
      this.additionalArtifacts = new ArrayList<VnfPackageArtifactInfo>();
    }
    this.additionalArtifacts.add(additionalArtifactsItem);
    return this;
  }

  /**
   * Get additionalArtifacts
   * @return additionalArtifacts
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<VnfPackageArtifactInfo> getAdditionalArtifacts() {
    return additionalArtifacts;
  }

  public void setAdditionalArtifacts(List<VnfPackageArtifactInfo> additionalArtifacts) {
    this.additionalArtifacts = additionalArtifacts;
  }

  public VnfPkgInfo onboardingState(PackageOnboardingStateType onboardingState) {
    this.onboardingState = onboardingState;
    return this;
  }

  /**
   * Get onboardingState
   * @return onboardingState
  **/
  @ApiModelProperty(value = "")

  @Valid

  public PackageOnboardingStateType getOnboardingState() {
    return onboardingState;
  }

  public void setOnboardingState(PackageOnboardingStateType onboardingState) {
    this.onboardingState = onboardingState;
  }

  public VnfPkgInfo operationalState(PackageOperationalStateType operationalState) {
    this.operationalState = operationalState;
    return this;
  }

  /**
   * Get operationalState
   * @return operationalState
  **/
  @ApiModelProperty(value = "")

  @Valid

  public PackageOperationalStateType getOperationalState() {
    return operationalState;
  }

  public void setOperationalState(PackageOperationalStateType operationalState) {
    this.operationalState = operationalState;
  }

  public VnfPkgInfo usageState(PackageUsageStateType usageState) {
    this.usageState = usageState;
    return this;
  }

  /**
   * Get usageState
   * @return usageState
  **/
  @ApiModelProperty(value = "")

  @Valid

  public PackageUsageStateType getUsageState() {
    return usageState;
  }

  public void setUsageState(PackageUsageStateType usageState) {
    this.usageState = usageState;
  }

  public VnfPkgInfo userDefinedData(KeyValuePairs userDefinedData) {
    this.userDefinedData = userDefinedData;
    return this;
  }

  /**
   * Get userDefinedData
   * @return userDefinedData
  **/
  @ApiModelProperty(value = "")

  @Valid

  public KeyValuePairs getUserDefinedData() {
    return userDefinedData;
  }

  public void setUserDefinedData(KeyValuePairs userDefinedData) {
    this.userDefinedData = userDefinedData;
  }

  public VnfPkgInfo links(String links) {
    this.links = links;
    return this;
  }

  /**
   * Get links
   * @return links
  **/
  @ApiModelProperty(value = "")


  public String getLinks() {
    return links;
  }

  public void setLinks(String links) {
    this.links = links;
  }

  public VnfPkgInfo self(String self) {
    this.self = self;
    return this;
  }

  /**
   * Get self
   * @return self
  **/
  @ApiModelProperty(value = "")


  public String getSelf() {
    return self;
  }

  public void setSelf(String self) {
    this.self = self;
  }

  public VnfPkgInfo vnfd(String vnfd) {
    this.vnfd = vnfd;
    return this;
  }

  /**
   * Get vnfd
   * @return vnfd
  **/
  @ApiModelProperty(value = "")


  public String getVnfd() {
    return vnfd;
  }

  public void setVnfd(String vnfd) {
    this.vnfd = vnfd;
  }

  public VnfPkgInfo packageContent(String packageContent) {
    this.packageContent = packageContent;
    return this;
  }

  /**
   * Get packageContent
   * @return packageContent
  **/
  @ApiModelProperty(value = "")


  public String getPackageContent() {
    return packageContent;
  }

  public void setPackageContent(String packageContent) {
    this.packageContent = packageContent;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VnfPkgInfo vnfPkgInfo = (VnfPkgInfo) o;
    return Objects.equals(this.id, vnfPkgInfo.id) &&
        Objects.equals(this.vnfdId, vnfPkgInfo.vnfdId) &&
        Objects.equals(this.vnfProvider, vnfPkgInfo.vnfProvider) &&
        Objects.equals(this.vnfProductName, vnfPkgInfo.vnfProductName) &&
        Objects.equals(this.vnfSoftwareVersion, vnfPkgInfo.vnfSoftwareVersion) &&
        Objects.equals(this.vnfdVersion, vnfPkgInfo.vnfdVersion) &&
        Objects.equals(this.checksum, vnfPkgInfo.checksum) &&
        Objects.equals(this.softwareImages, vnfPkgInfo.softwareImages) &&
        Objects.equals(this.additionalArtifacts, vnfPkgInfo.additionalArtifacts) &&
        Objects.equals(this.onboardingState, vnfPkgInfo.onboardingState) &&
        Objects.equals(this.operationalState, vnfPkgInfo.operationalState) &&
        Objects.equals(this.usageState, vnfPkgInfo.usageState) &&
        Objects.equals(this.userDefinedData, vnfPkgInfo.userDefinedData) &&
        Objects.equals(this.links, vnfPkgInfo.links) &&
        Objects.equals(this.self, vnfPkgInfo.self) &&
        Objects.equals(this.vnfd, vnfPkgInfo.vnfd) &&
        Objects.equals(this.packageContent, vnfPkgInfo.packageContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, vnfdId, vnfProvider, vnfProductName, vnfSoftwareVersion, vnfdVersion, checksum, softwareImages, additionalArtifacts, onboardingState, operationalState, usageState, userDefinedData, links, self, vnfd, packageContent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VnfPkgInfo {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    vnfdId: ").append(toIndentedString(vnfdId)).append("\n");
    sb.append("    vnfProvider: ").append(toIndentedString(vnfProvider)).append("\n");
    sb.append("    vnfProductName: ").append(toIndentedString(vnfProductName)).append("\n");
    sb.append("    vnfSoftwareVersion: ").append(toIndentedString(vnfSoftwareVersion)).append("\n");
    sb.append("    vnfdVersion: ").append(toIndentedString(vnfdVersion)).append("\n");
    sb.append("    checksum: ").append(toIndentedString(checksum)).append("\n");
    sb.append("    softwareImages: ").append(toIndentedString(softwareImages)).append("\n");
    sb.append("    additionalArtifacts: ").append(toIndentedString(additionalArtifacts)).append("\n");
    sb.append("    onboardingState: ").append(toIndentedString(onboardingState)).append("\n");
    sb.append("    operationalState: ").append(toIndentedString(operationalState)).append("\n");
    sb.append("    usageState: ").append(toIndentedString(usageState)).append("\n");
    sb.append("    userDefinedData: ").append(toIndentedString(userDefinedData)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
    sb.append("    self: ").append(toIndentedString(self)).append("\n");
    sb.append("    vnfd: ").append(toIndentedString(vnfd)).append("\n");
    sb.append("    packageContent: ").append(toIndentedString(packageContent)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

