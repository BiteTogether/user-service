package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;

public interface FriendRequestService {
  ApiResponseDTO<Long> createFriendRequest(Long receiverId);

  ApiResponseDTO<FriendResponse> acceptFriendRequest(Long id);

  ApiResponseDTO<String> deleteFriendRequest(Long id);

  ApiResponsePaginationDTO<FriendRequestResponse> getSentFriendRequests(int page, int size);

  ApiResponsePaginationDTO<FriendRequestResponse> getReceivedFriendRequests(int page, int size);
}
