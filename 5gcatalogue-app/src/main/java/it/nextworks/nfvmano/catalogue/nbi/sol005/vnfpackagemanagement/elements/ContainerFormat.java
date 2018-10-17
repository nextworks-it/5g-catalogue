package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets ContainerFormat
 */
public enum ContainerFormat {
  
  AKI("AKI"),
  
  AMI("AMI"),
  
  ARI("ARI"),
  
  BARE("BARE"),
  
  DOCKER("DOCKER"),
  
  OVA_OVF("OVA_OVF"),
  
  OVF_OVF("OVF_OVF");

  private String value;

  ContainerFormat(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ContainerFormat fromValue(String text) {
    for (ContainerFormat b : ContainerFormat.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

