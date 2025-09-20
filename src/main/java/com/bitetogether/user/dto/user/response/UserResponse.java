package com.bitetogether.user.dto.user.response;

import com.bitetogether.common.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
  private Long id;
  private String username;
  private String email;
  private String fullName;
  private String phoneNumber;
  private String avatar;
  private Role role;
}
