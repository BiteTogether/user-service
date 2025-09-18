package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;

public interface UserService {
  ApiResponse<Long> createUser(CreateUserRequest createUserRequest);
}
