package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdatePhoneRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserNotificationSettingsRequest;
import com.bitetogether.user.dto.user.request.UserOnlineStatus;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.request.ValidateUserCriteriaRequest;
import com.bitetogether.user.dto.user.response.ListUserDetailsResponse;
import com.bitetogether.user.dto.user.response.UpdatePhoneResponse;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserNotificationResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.dto.user.response.ValidateUserCriteriaResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
  ApiResponseDTO<Long> createUser(CreateUserRequest createUserRequest);

  ApiResponseDTO<UserResponse> updateUser(Long id, UpdateUserRequest updateUserRequest);

  ApiResponseDTO<String> deleteUser(Long id);

  ApiResponseDTO<UserDetailsResponse> getCurrentUser();

  ApiResponseDTO<UserGetByIdResponse> getUserById(Long id);

  ApiResponseDTO<UserSearchResponse> searchUsersWithFilter(UserSearchRequest userSearchRequest);

  ApiResponseDTO<UserNotificationResponse> getNotificationSettings(Long id);

  ApiResponseDTO<Void> updateNotificationSettings(
      Long id, UserNotificationSettingsRequest userNotificationSettingsRequest);

  ApiResponseDTO<Void> setUserOnline(Long id, UserOnlineStatus userOnlineStatus);

  ApiResponseDTO<ListUserDetailsResponse> getListUser(List<Long> request);

  ApiResponseDTO<String> uploadAvatar(Long userId, MultipartFile file);

  ApiResponseDTO<Void> deleteAvatar(Long userId);

  ApiResponseDTO<ValidateUserCriteriaResponse> validateUserCriteria(
      ValidateUserCriteriaRequest criteria);

  ApiResponseDTO<UpdatePhoneResponse> updatePhone(UpdatePhoneRequest updatePhoneRequest);
}
