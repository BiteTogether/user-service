package com.bitetogether.user.dto.friend.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendResponse {
  private Long id;
  private String username;
  private String fullName;
  private String avatar;
  private String conversationId;
}
