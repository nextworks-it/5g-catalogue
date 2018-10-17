package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets PackageOnboardingStateType
 */
public enum PackageOnboardingStateType {
  
  CREATED("CREATED"),
  
  UPLOADING("UPLOADING"),
  
  PROCESSING("PROCESSING"),
  
  ONBOARDED("ONBOARDED");

  private String value;

  PackageOnboardingStateType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PackageOnboardingStateType fromValue(String text) {
    for (PackageOnboardingStateType b : PackageOnboardingStateType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

