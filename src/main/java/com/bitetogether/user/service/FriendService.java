package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import org.springframework.data.domain.Page;

public interface FriendService {
  ApiResponse<Page<FriendResponse>> getFriendsList(PaginationRequest paginationRequest);

  ApiResponse<String> deleteFriend(Long id);
}
