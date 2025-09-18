package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class CreateUserRequest {
  @NotBlank(message = "Username can not be empty")
  String username;

  @NotBlank(message = "Email can not be empty")
  @Email(message = "Email format is invalid")
  String email;

  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$",
      message = "Password must be at least 8 characters, include letters and numbers")
  String password;

  @NotBlank(message = "Fullname can not be empty")
  String fullName;

  @NotBlank(message = "Phone number can not be empty")
  String phoneNumber;

  String avatar;
}
