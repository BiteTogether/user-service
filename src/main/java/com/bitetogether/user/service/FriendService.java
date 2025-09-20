package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.friend.response.FriendResponse;

import java.util.List;

public interface FriendService {
    ApiResponse<List<FriendResponse>> getFriendsList();

    ApiResponse<String> deleteFriend(Long id);
}
