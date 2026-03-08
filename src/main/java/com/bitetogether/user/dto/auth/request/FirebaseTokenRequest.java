package com.bitetogether.user.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirebaseTokenRequest {
  @NotBlank(message = "Firebase ID Token cannot be empty")
  String idToken;
}
