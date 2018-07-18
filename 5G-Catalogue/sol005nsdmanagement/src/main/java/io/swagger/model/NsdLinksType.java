package io.swagger.model;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-18T14:48:06.806+02:00")

public class NsdLinksType   {
  @JsonProperty("self")
  private String self = null;

  @JsonProperty("nsd_content")
  private String nsdContent = null;

  public NsdLinksType self(String self) {
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

  public NsdLinksType nsdContent(String nsdContent) {
    this.nsdContent = nsdContent;
    return this;
  }

  /**
   * Get nsdContent
   * @return nsdContent
  **/
  @ApiModelProperty(value = "")


  public String getNsdContent() {
    return nsdContent;
  }

  public void setNsdContent(String nsdContent) {
    this.nsdContent = nsdContent;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NsdLinksType nsdLinksType = (NsdLinksType) o;
    return Objects.equals(this.self, nsdLinksType.self) &&
        Objects.equals(this.nsdContent, nsdLinksType.nsdContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(self, nsdContent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NsdLinksType {\n");
    
    sb.append("    self: ").append(toIndentedString(self)).append("\n");
    sb.append("    nsdContent: ").append(toIndentedString(nsdContent)).append("\n");
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

