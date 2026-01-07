package com.bitetogether.user.dto.auth.response;

import com.bitetogether.common.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserInfoResponse {
  private Long id;
  private String username;
  private String email;
  private String fullName;
  private String phoneNumber;
  private String avatar;
  private Role role;
}
