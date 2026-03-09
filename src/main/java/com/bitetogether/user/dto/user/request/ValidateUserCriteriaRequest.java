package com.bitetogether.user.dto.user.request;

import com.bitetogether.user.enums.ValidateCriteria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateUserCriteriaRequest {
  @NotNull private ValidateCriteria criteriaType;

  @NotBlank private String criteriaValue;
}
