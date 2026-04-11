package com.bitetogether.user.dto.user.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserNotificationResponse {
  @JsonProperty("pushNotificationsEnabled")
  private boolean pushNotificationsEnabled;
}
