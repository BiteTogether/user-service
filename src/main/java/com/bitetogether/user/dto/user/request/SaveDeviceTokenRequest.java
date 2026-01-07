package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveDeviceTokenRequest {
  @NotBlank(message = "Device token can not be empty")
  private String deviceToken;
}
