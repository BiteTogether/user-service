package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;

public interface FriendRequestService {
  ApiResponse<Long> createFriendRequest(Long receiverId);

  ApiResponse<Void> acceptFriendRequest(Long id);

  ApiResponse<String> deleteFriendRequest(Long id);

  ApiResponsePagination<FriendRequestResponse> getSentFriendRequests(int page, int size);

  ApiResponsePagination<FriendRequestResponse> getReceivedFriendRequests(int page, int size);
}
