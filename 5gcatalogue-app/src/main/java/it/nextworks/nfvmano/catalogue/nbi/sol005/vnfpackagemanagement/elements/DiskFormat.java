package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets DiskFormat
 */
public enum DiskFormat {
  
  AKI("AKI"),
  
  AMI("AMI"),
  
  ARI("ARI"),
  
  ISO("ISO"),
  
  QCOW2("QCOW2"),
  
  RAW("RAW"),
  
  VDI("VDI"),
  
  VHD("VHD"),
  
  VHDX("VHDX"),
  
  VMDK("VMDK");

  private String value;

  DiskFormat(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static DiskFormat fromValue(String text) {
    for (DiskFormat b : DiskFormat.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

