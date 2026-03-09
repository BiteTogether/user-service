package com.bitetogether.user.dto.user.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UpdatePhoneResponse {
  private Long userId;
  private String phoneNumber;
  private String firebaseUid;
}
