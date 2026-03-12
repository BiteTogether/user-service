package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
  @NotBlank(message = "Username can not be empty")
  @Pattern(
      regexp = "^[a-zA-Z0-9._]{6,20}$",
      message =
          "Username must be 6-20 characters, and can only contain letters, numbers, dots, and underscores")
  String username;

  @NotBlank(message = "Firebase UID cannot be empty")
  String firebaseUid;

  @NotBlank(message = "Fullname can not be empty")
  String fullName;

  @NotBlank(message = "Phone number cannot be empty")
  @Pattern(regexp = "^\\d{9,11}$", message = "Phone number must contain 9 to 11 digits only")
  private String phoneNumber;

  String avatar;

  String role;
}
