package com.bitetogether.user.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchRequest {
  @NotBlank(message = "Searching keyword must not be blank")
  String keyword;
}
