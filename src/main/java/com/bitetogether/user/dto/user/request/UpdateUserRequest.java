package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {
  @NotBlank(message = "Username cannot be empty")
  private String username;

  private String fullName;

  private String avatar;
}
