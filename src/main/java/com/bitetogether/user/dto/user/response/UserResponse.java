package com.bitetogether.user.dto.user.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserResponse {
  private Long id;
  private String username;
  private String fullName;
  private String phoneNumber;
  private String role;
}
