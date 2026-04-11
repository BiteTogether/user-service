package com.bitetogether.user.dto.user.request;

import com.bitetogether.user.enums.UserState;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserState {
  @NotNull(message = "User state must not be null")
  private UserState state;
}
