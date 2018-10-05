package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * PkgmLinks
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-10-05T11:50:31.473+02:00")

public class PkgmLinks   {
  @JsonProperty("vnfPackage")
  private String vnfPackage = null;

  @JsonProperty("subscription")
  private String subscription = null;

  public PkgmLinks vnfPackage(String vnfPackage) {
    this.vnfPackage = vnfPackage;
    return this;
  }

  /**
   * Get vnfPackage
   * @return vnfPackage
  **/
  @ApiModelProperty(value = "")


  public String getVnfPackage() {
    return vnfPackage;
  }

  public void setVnfPackage(String vnfPackage) {
    this.vnfPackage = vnfPackage;
  }

  public PkgmLinks subscription(String subscription) {
    this.subscription = subscription;
    return this;
  }

  /**
   * Get subscription
   * @return subscription
  **/
  @ApiModelProperty(value = "")


  public String getSubscription() {
    return subscription;
  }

  public void setSubscription(String subscription) {
    this.subscription = subscription;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PkgmLinks pkgmLinks = (PkgmLinks) o;
    return Objects.equals(this.vnfPackage, pkgmLinks.vnfPackage) &&
        Objects.equals(this.subscription, pkgmLinks.subscription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vnfPackage, subscription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PkgmLinks {\n");
    
    sb.append("    vnfPackage: ").append(toIndentedString(vnfPackage)).append("\n");
    sb.append("    subscription: ").append(toIndentedString(subscription)).append("\n");
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

