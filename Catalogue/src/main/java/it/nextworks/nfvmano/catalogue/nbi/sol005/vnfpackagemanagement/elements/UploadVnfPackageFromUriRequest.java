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
 * UploadVnfPackageFromUriRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-10-05T11:50:31.473+02:00")

public class UploadVnfPackageFromUriRequest   {
  @JsonProperty("addressInformation")
  private String addressInformation = null;

  @JsonProperty("userName")
  private String userName = null;

  @JsonProperty("password")
  private String password = null;

  public UploadVnfPackageFromUriRequest addressInformation(String addressInformation) {
    this.addressInformation = addressInformation;
    return this;
  }

  /**
   * Get addressInformation
   * @return addressInformation
  **/
  @ApiModelProperty(value = "")


  public String getAddressInformation() {
    return addressInformation;
  }

  public void setAddressInformation(String addressInformation) {
    this.addressInformation = addressInformation;
  }

  public UploadVnfPackageFromUriRequest userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * Get userName
   * @return userName
  **/
  @ApiModelProperty(value = "")


  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public UploadVnfPackageFromUriRequest password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
  **/
  @ApiModelProperty(value = "")


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadVnfPackageFromUriRequest uploadVnfPackageFromUriRequest = (UploadVnfPackageFromUriRequest) o;
    return Objects.equals(this.addressInformation, uploadVnfPackageFromUriRequest.addressInformation) &&
        Objects.equals(this.userName, uploadVnfPackageFromUriRequest.userName) &&
        Objects.equals(this.password, uploadVnfPackageFromUriRequest.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addressInformation, userName, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadVnfPackageFromUriRequest {\n");
    
    sb.append("    addressInformation: ").append(toIndentedString(addressInformation)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
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

