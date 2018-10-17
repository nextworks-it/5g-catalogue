package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets PackageChangeType
 */
public enum PackageChangeType {
  
  OP_STATE_CHANGE("OP_STATE_CHANGE"),
  
  PKG_DELETE("PKG_DELETE");

  private String value;

  PackageChangeType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PackageChangeType fromValue(String text) {
    for (PackageChangeType b : PackageChangeType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

