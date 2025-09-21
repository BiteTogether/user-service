package com.bitetogether.user.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
  @NotBlank(message = "Refresh token is required")
  @JsonProperty("refresh_token")
  private String refreshToken;
}
