package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;

public interface FriendRequestService {
  ApiResponse<Long> createFriendRequest(Long receiverId);

  ApiResponse<Void> acceptFriendRequest(Long id);

  ApiResponse<String> deleteFriendRequest(Long id);

  ApiResponsePagination<FriendRequestResponse> getSentFriendRequests(
      PaginationRequest paginationRequest);

  ApiResponsePagination<FriendRequestResponse> getReceivedFriendRequests(
      PaginationRequest paginationRequest);
}
