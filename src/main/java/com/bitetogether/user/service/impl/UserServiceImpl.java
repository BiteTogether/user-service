package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;
import static com.bitetogether.common.util.SecurityUtils.hasRole;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.ErrorCode;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
  UserRepository userRepository;
  UserMapper userMapper;
  PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public ApiResponse<Long> createUser(CreateUserRequest createUserRequest) {
    validateCreateUserRequest(createUserRequest);

    User newUser = userMapper.toEntity(createUserRequest);

    handlePassword(newUser);
    handleRole(newUser);

    User databaseUser = userRepository.save(newUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User created successfully", databaseUser.getId());
  }

  @Override
  @Transactional
  public ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest updateUserRequest) {
    User existingUser = findUserById(id);

    validateUserAuthorization(id);

    validateUpdateUserRequest(updateUserRequest, existingUser);

    userMapper.updateUserFromRequest(updateUserRequest, existingUser);

    User updatedUser = userRepository.save(existingUser);
    UserResponse userResponse = userMapper.toResponse(updatedUser);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User updated successfully", userResponse);
  }

  @Override
  @Transactional
  public ApiResponse<String> deleteUser(Long id) {
    User existingUser = findUserById(id);

    validateUserAuthorization(id);

    userRepository.delete(existingUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User with id " + id + " has been deleted successfully", null);
  }

  @Override
  public ApiResponse<UserResponse> getCurrentUser() {
    Long currentUserId = getCurrentUserId();

    User currentUser = findUserById(currentUserId);

    UserResponse userResponse = userMapper.toResponse(currentUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Your account's information has been fetched successfully",
        userResponse);
  }

  @Override
  public ApiResponse<UserResponse> getUserById(Long id) {
    validateGetUserByIdRequest(id);

    User user = findUserById(id);

    UserResponse userResponse = userMapper.toResponse(user);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "User's information has been fetched successfully",
        userResponse);
  }

  private User findUserById(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private void validateCreateUserRequest(CreateUserRequest createUserRequest) {
    if (userRepository.existsByEmail(createUserRequest.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }
    if (userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())) {
      throw new AppException(ErrorCode.PHONE_EXISTED);
    }
  }

  private void validateUpdateUserRequest(UpdateUserRequest updateUserRequest, User existingUser) {
    if (updateUserRequest.getUsername() != null
        && !updateUserRequest.getUsername().equals(existingUser.getUsername())
        && userRepository.existsByUsername(updateUserRequest.getUsername())) {
      throw new AppException(ErrorCode.USERNAME_EXISTED);
    }
  }

  private void validateGetUserByIdRequest(Long id) {
    if (hasRole(Role.USER.name())) {
      Long currentUserId = getCurrentUserId();
      if (currentUserId.equals(id)) {
        return;
      }
      User currentUser = findUserById(currentUserId);
      if (currentUser.getFriendList().stream().noneMatch(friend -> friend.getId().equals(id))) {
        throw new AppException(ErrorCode.USER_FORBIDDEN);
      }
    }
  }

  private void validateUserAuthorization(Long id) {
    if (hasRole(Role.USER.name())) {
      Long currentUserId = getCurrentUserId();
      if (!currentUserId.equals(id)) {
        throw new AppException(ErrorCode.USER_FORBIDDEN);
      }
    }
  }

  private void handlePassword(User newUser) {
    String encodedPassword = passwordEncoder.encode(newUser.getPassword());
    newUser.setPassword(encodedPassword);
  }

  private void handleRole(User newUser) {
    if (newUser.getRole() == null) {
      newUser.setRole(Role.USER.name());
    }
  }
}
