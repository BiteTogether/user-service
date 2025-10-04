package com.bitetogether.user.dto.friendrequest.response;

import com.bitetogether.user.dto.friend.response.FriendResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendRequestResponse {
  private Long id;
  private FriendResponse user;
}
