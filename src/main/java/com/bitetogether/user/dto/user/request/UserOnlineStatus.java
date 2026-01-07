package com.bitetogether.user.dto.user.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserOnlineStatus {
  @NotNull(message = "Online status must not be null")
  @JsonProperty("isOnline")
  private boolean isOnline;
}
