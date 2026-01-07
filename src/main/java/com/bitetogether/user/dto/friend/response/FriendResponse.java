package com.bitetogether.user.dto.friend.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendResponse {
  Long id;
  String username;
  String fullName;
  String avatar;
}
