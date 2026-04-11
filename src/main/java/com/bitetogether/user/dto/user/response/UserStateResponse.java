package com.bitetogether.user.dto.user.response;

import com.bitetogether.user.enums.UserState;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStateResponse {
  private UserState state;
  private LocalDateTime lastSeen;
  private boolean pushNotificationsEnabled;
}
