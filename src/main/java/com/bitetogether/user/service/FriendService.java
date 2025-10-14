package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.user.dto.friend.response.FriendResponse;

public interface FriendService {
  ApiResponsePagination<FriendResponse> getFriendsList(PaginationRequest paginationRequest);

  ApiResponse<String> deleteFriend(Long id);
}
