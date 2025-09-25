package com.bitetogether.user.dto.friendrequest.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FriendRequestResponse {
  private Long id;
  private String username;
  private String fullName;
  private String avatar;
}
