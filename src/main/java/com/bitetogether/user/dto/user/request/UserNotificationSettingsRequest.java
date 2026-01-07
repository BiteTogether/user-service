package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserNotificationSettingsRequest {
  @NotNull(message = "Push notifications setting must not be null")
  private boolean pushNotificationsEnabled;

  @NotNull(message = "In-app notifications setting must not be null")
  private boolean inAppNotificationsEnabled;
}
