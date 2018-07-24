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
 * Links to resources related to this resource.
 */
@ApiModel(description = "Links to resources related to this resource.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

public class PnfdLinksType   {
  @JsonProperty("self")
  private String self = null;

  @JsonProperty("pnfd_content")
  private String pnfdContent = null;

  public PnfdLinksType self(String self) {
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

  public PnfdLinksType pnfdContent(String pnfdContent) {
    this.pnfdContent = pnfdContent;
    return this;
  }

  /**
   * Get pnfdContent
   * @return pnfdContent
  **/
  @ApiModelProperty(value = "")


  public String getPnfdContent() {
    return pnfdContent;
  }

  public void setPnfdContent(String pnfdContent) {
    this.pnfdContent = pnfdContent;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PnfdLinksType pnfdLinksType = (PnfdLinksType) o;
    return Objects.equals(this.self, pnfdLinksType.self) &&
        Objects.equals(this.pnfdContent, pnfdLinksType.pnfdContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(self, pnfdContent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PnfdLinksType {\n");
    
    sb.append("    self: ").append(toIndentedString(self)).append("\n");
    sb.append("    pnfdContent: ").append(toIndentedString(pnfdContent)).append("\n");
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

