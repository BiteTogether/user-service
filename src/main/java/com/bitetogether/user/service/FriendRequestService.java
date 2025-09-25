package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.user.dto.friendrequest.request.CreateFriendRequestRequest;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;

import java.util.List;

public interface FriendRequestService {
    ApiResponse<Long> createFriendRequest(CreateFriendRequestRequest createFriendRequestRequest);

    ApiResponse<Void> acceptFriendRequest(Long id);

    ApiResponse<String> deleteFriendRequest(Long id);

    ApiResponsePagination<List<FriendRequestResponse>> getSentFriendRequests(PaginationRequest paginationRequest);

    ApiResponsePagination<List<FriendRequestResponse>> getReceivedFriendRequests(PaginationRequest paginationRequest);
}
