package com.bitetogether.user.dto.user.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ValidateUserCriteriaResponse {
  private boolean isValid;
  private String validationMessage;
}
