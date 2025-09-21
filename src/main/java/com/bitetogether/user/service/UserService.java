package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.response.UserResponse;

public interface UserService {
  ApiResponse<Long> createUser(CreateUserRequest createUserRequest);

  ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest updateUserRequest);

  ApiResponse<String> deleteUser(Long id);

  ApiResponse<UserResponse> getCurrentUser();

  ApiResponse<UserResponse> getUserById(Long id);
}
