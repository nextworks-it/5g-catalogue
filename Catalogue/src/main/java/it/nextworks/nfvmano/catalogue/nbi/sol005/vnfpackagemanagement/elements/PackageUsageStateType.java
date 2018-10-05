package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets PackageUsageStateType
 */
public enum PackageUsageStateType {
  
  IN_USE("IN_USE"),
  
  NOT_IN_USE("NOT_IN_USE");

  private String value;

  PackageUsageStateType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PackageUsageStateType fromValue(String text) {
    for (PackageUsageStateType b : PackageUsageStateType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

