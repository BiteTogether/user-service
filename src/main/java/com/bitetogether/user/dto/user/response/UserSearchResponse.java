package com.bitetogether.user.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchResponse {
  Long id;
  String username;
  String fullName;
  String avatar;

  // Friend status fields
  private Boolean hasFriendRequestSent;
  private Boolean hasFriendRequestReceived;
  private Long friendRequestId;
  private Boolean isFriend;
}
