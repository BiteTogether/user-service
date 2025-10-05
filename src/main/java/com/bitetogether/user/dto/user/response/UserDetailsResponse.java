package com.bitetogether.user.dto.user.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserDetailsResponse {
  private Long id;
  private String username;
  private String email;
  private String fullName;
  private String phoneNumber;
  private String avatar;
  private String role;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
