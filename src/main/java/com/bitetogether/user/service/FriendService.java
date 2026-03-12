package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.user.dto.friend.response.FriendResponse;

public interface FriendService {
  ApiResponsePaginationDTO<FriendResponse> getFriendsList(int page, int size);

  ApiResponseDTO<String> deleteFriend(Long id);
}
