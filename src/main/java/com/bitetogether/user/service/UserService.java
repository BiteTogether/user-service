package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserNotificationSettingsRequest;
import com.bitetogether.user.dto.user.request.UserOnlineStatus;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserNotificationResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;

public interface UserService {
  ApiResponse<Long> createUser(CreateUserRequest createUserRequest);

  ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest updateUserRequest);

  ApiResponse<String> deleteUser(Long id);

  ApiResponse<UserDetailsResponse> getCurrentUser();

  ApiResponse<UserGetByIdResponse> getUserById(Long id);

  ApiResponse<UserSearchResponse> searchUsersWithFilter(UserSearchRequest userSearchRequest);

  ApiResponse<UserNotificationResponse> getNotificationSettings(Long id);

  ApiResponse<Void> updateNotificationSettings(
      Long id, UserNotificationSettingsRequest userNotificationSettingsRequest);

  ApiResponse<Void> setUserOnline(Long id, UserOnlineStatus userOnlineStatus);
}
