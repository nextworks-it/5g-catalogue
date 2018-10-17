package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets NotificationType
 */
public enum NotificationType {
  
  ONBOARDING_NOTIFICATION("VNF_PACKAGE_ONBOARDING_NOTIFICATION"),
  
  CHANGE_NOTIFICATION("VNF_PACKAGE_CHANGE_NOTIFICATION");

  private String value;

  NotificationType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static NotificationType fromValue(String text) {
    for (NotificationType b : NotificationType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

