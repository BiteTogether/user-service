package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePhoneRequest {
  @NotBlank(message = "Firebase ID Token cannot be empty")
  private String idToken;
}
