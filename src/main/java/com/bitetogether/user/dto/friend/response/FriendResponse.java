package com.bitetogether.user.dto.friend.response;

import lombok.Builder;

@Builder
public class FriendResponse {
    Long id;
    String username;
    String fullName;
    String avatar;
}
