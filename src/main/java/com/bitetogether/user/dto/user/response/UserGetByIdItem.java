package com.bitetogether.user.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGetByIdItem {
  private Boolean hasFriendRequestSent;
  private Boolean hasFriendRequestReceived;
  private Long friendRequestId;
  private Boolean isFriend;
  private Boolean isUserOnline = null;
  private LocalDateTime lastSeenUser = null;
}
