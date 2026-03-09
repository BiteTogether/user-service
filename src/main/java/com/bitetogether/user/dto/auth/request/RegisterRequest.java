package com.bitetogether.user.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
  @NotBlank(message = "ID token is required")
  private String idToken;

  private String username;

  private String fullName;
}
