package com.bitetogether.user.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
  @NotBlank(message = "Username can not be empty")
  String email;

  @NotBlank(message = "Password can not be empty")
  String password;
}
