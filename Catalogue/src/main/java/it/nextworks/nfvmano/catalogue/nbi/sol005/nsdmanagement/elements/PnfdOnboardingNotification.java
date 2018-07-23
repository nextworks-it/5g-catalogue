package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.model.NotificationType;
import io.swagger.model.PnfdmLinks;
import java.util.UUID;
import org.threeten.bp.OffsetDateTime;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This type represents a PNFD management notification, which informs the receiver of the successful on-boarding of a PNFD. It shall comply with the provisions defined in Table 5.5.2.13-1. The support of this notification is mandatory. The notification is triggered when a new PNFD is on-boarded.
 */
@ApiModel(description = "This type represents a PNFD management notification, which informs the receiver of the successful on-boarding of a PNFD. It shall comply with the provisions defined in Table 5.5.2.13-1. The support of this notification is mandatory. The notification is triggered when a new PNFD is on-boarded.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

public class PnfdOnboardingNotification   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("notificationType")
  private NotificationType notificationType = null;

  @JsonProperty("subscriptionId")
  private UUID subscriptionId = null;

  @JsonProperty("timeStamp")
  private OffsetDateTime timeStamp = null;

  @JsonProperty("pnfdInfoId")
  private UUID pnfdInfoId = null;

  @JsonProperty("pnfdId")
  private UUID pnfdId = null;

  @JsonProperty("_links")
  private PnfdmLinks links = null;

  public PnfdOnboardingNotification id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PnfdOnboardingNotification notificationType(NotificationType notificationType) {
    this.notificationType = notificationType;
    return this;
  }

  /**
   * Discriminator for the different notification types. Shall be set to \"PnfdOnboardingNotification\" for this notification type.
   * @return notificationType
  **/
  @ApiModelProperty(required = true, value = "Discriminator for the different notification types. Shall be set to \"PnfdOnboardingNotification\" for this notification type.")
  @NotNull

  @Valid

  public NotificationType getNotificationType() {
    return notificationType;
  }

  public void setNotificationType(NotificationType notificationType) {
    this.notificationType = notificationType;
  }

  public PnfdOnboardingNotification subscriptionId(UUID subscriptionId) {
    this.subscriptionId = subscriptionId;
    return this;
  }

  /**
   * Get subscriptionId
   * @return subscriptionId
  **/
  @ApiModelProperty(value = "")

  @Valid

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(UUID subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public PnfdOnboardingNotification timeStamp(OffsetDateTime timeStamp) {
    this.timeStamp = timeStamp;
    return this;
  }

  /**
   * Date-time of the generation of the notification.
   * @return timeStamp
  **/
  @ApiModelProperty(required = true, value = "Date-time of the generation of the notification.")
  @NotNull

  @Valid

  public OffsetDateTime getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(OffsetDateTime timeStamp) {
    this.timeStamp = timeStamp;
  }

  public PnfdOnboardingNotification pnfdInfoId(UUID pnfdInfoId) {
    this.pnfdInfoId = pnfdInfoId;
    return this;
  }

  /**
   * Get pnfdInfoId
   * @return pnfdInfoId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public UUID getPnfdInfoId() {
    return pnfdInfoId;
  }

  public void setPnfdInfoId(UUID pnfdInfoId) {
    this.pnfdInfoId = pnfdInfoId;
  }

  public PnfdOnboardingNotification pnfdId(UUID pnfdId) {
    this.pnfdId = pnfdId;
    return this;
  }

  /**
   * Get pnfdId
   * @return pnfdId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public UUID getPnfdId() {
    return pnfdId;
  }

  public void setPnfdId(UUID pnfdId) {
    this.pnfdId = pnfdId;
  }

  public PnfdOnboardingNotification links(PnfdmLinks links) {
    this.links = links;
    return this;
  }

  /**
   * This type represents the links to resources that a PNFD management notification can contain.
   * @return links
  **/
  @ApiModelProperty(required = true, value = "This type represents the links to resources that a PNFD management notification can contain.")
  @NotNull

  @Valid

  public PnfdmLinks getLinks() {
    return links;
  }

  public void setLinks(PnfdmLinks links) {
    this.links = links;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PnfdOnboardingNotification pnfdOnboardingNotification = (PnfdOnboardingNotification) o;
    return Objects.equals(this.id, pnfdOnboardingNotification.id) &&
        Objects.equals(this.notificationType, pnfdOnboardingNotification.notificationType) &&
        Objects.equals(this.subscriptionId, pnfdOnboardingNotification.subscriptionId) &&
        Objects.equals(this.timeStamp, pnfdOnboardingNotification.timeStamp) &&
        Objects.equals(this.pnfdInfoId, pnfdOnboardingNotification.pnfdInfoId) &&
        Objects.equals(this.pnfdId, pnfdOnboardingNotification.pnfdId) &&
        Objects.equals(this.links, pnfdOnboardingNotification.links);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, notificationType, subscriptionId, timeStamp, pnfdInfoId, pnfdId, links);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PnfdOnboardingNotification {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    notificationType: ").append(toIndentedString(notificationType)).append("\n");
    sb.append("    subscriptionId: ").append(toIndentedString(subscriptionId)).append("\n");
    sb.append("    timeStamp: ").append(toIndentedString(timeStamp)).append("\n");
    sb.append("    pnfdInfoId: ").append(toIndentedString(pnfdInfoId)).append("\n");
    sb.append("    pnfdId: ").append(toIndentedString(pnfdId)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
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

