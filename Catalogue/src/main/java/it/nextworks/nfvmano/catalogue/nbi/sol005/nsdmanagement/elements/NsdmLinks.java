package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This type represents the links to resources that an NSD management notification can contain.
 */
@ApiModel(description = "This type represents the links to resources that an NSD management notification can contain.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

public class NsdmLinks   {
  @JsonProperty("nsdInfo")
  private String nsdInfo = null;

  @JsonProperty("subscription")
  private String subscription = null;

  public NsdmLinks nsdInfo(String nsdInfo) {
    this.nsdInfo = nsdInfo;
    return this;
  }

  /**
   * Get nsdInfo
   * @return nsdInfo
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getNsdInfo() {
    return nsdInfo;
  }

  public void setNsdInfo(String nsdInfo) {
    this.nsdInfo = nsdInfo;
  }

  public NsdmLinks subscription(String subscription) {
    this.subscription = subscription;
    return this;
  }

  /**
   * Get subscription
   * @return subscription
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


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
    NsdmLinks nsdmLinks = (NsdmLinks) o;
    return Objects.equals(this.nsdInfo, nsdmLinks.nsdInfo) &&
        Objects.equals(this.subscription, nsdmLinks.subscription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nsdInfo, subscription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NsdmLinks {\n");
    
    sb.append("    nsdInfo: ").append(toIndentedString(nsdInfo)).append("\n");
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

