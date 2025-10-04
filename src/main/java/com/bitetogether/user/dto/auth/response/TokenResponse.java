package com.bitetogether.user.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TokenResponse {
  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("refresh_expires_in")
  private Long refreshExpiresIn;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("session_state")
  private String sessionState;
}
