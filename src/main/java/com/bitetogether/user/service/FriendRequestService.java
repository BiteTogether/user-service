package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.friendrequest.request.CreateFriendRequestRequest;

public interface FriendRequestService {
    ApiResponse<Long> createFriendRequest(CreateFriendRequestRequest createFriendRequestRequest);

    ApiResponse<Void> acceptFriendRequest(Long id);

    ApiResponse<String> deleteFriendRequest(Long id);
}
