package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Match particular notification types. Permitted values: NsdOnBoardingNotification, NsdOnboardingFailureNotification, NsdChangeNotification, NsdDeletionNotification PnfdOnBoardingNotification, PnfdOnBoardingFailureNotification, PnfdDeletionNotification.  The permitted values of the \"notificationTypes\" ] attribute are spelled exactly as the names of the notification types to facilitate automated code generation systems.
 */
public enum NotificationType {
  
  NSDONBOARDINGNOTIFICATION("NsdOnBoardingNotification"),
  
  NSDONBOARDINGFAILURENOTIFICATION("NsdOnboardingFailureNotification"),
  
  NSDCHANGENOTIFICATION("NsdChangeNotification"),
  
  NSDDELETIONNOTIFICATION("NsdDeletionNotification"),
  
  PNFDONBOARDINGNOTIFICATION("PnfdOnBoardingNotification"),
  
  PNFDONBOARDINGFAILURENOTIFICATION("PnfdOnBoardingFailureNotification"),
  
  PNFDDELETIONNOTIFICATION("PnfdDeletionNotification");

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

