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
import com.bitetogether.user.util.UserHelper;
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
  UserHelper userHelper;

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
    User existingUser = userHelper.findUserById(id);

    validateUserAuthorization(id);

    validateUpdateUserRequest(updateUserRequest, existingUser);

    userMapper.updateUserFromRequest(updateUserRequest, existingUser);

    User updatedUser = userRepository.save(existingUser);
    UserResponse userResponse = userMapper.toUserResponse(updatedUser);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User updated successfully", userResponse);
  }

  @Override
  @Transactional
  public ApiResponse<String> deleteUser(Long id) {
    User existingUser = userHelper.findUserById(id);

    validateUserAuthorization(id);

    userRepository.delete(existingUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User with id " + id + " has been deleted successfully", null);
  }

  @Override
  public ApiResponse<UserResponse> getCurrentUser() {
    Long currentUserId = getCurrentUserId();

    User currentUser = userHelper.findUserById(currentUserId);

    UserResponse userResponse = userMapper.toUserResponse(currentUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Your account's information has been fetched successfully",
        userResponse);
  }

  @Override
  public ApiResponse<UserResponse> getUserById(Long id) {
    validateGetUserByIdRequest(id);

    User user = userHelper.findUserById(id);

    UserResponse userResponse = userMapper.toUserResponse(user);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "User's information has been fetched successfully",
        userResponse);
  }

  private void validateCreateUserRequest(CreateUserRequest createUserRequest) {
    String email = createUserRequest.getEmail();
    String phoneNumber = createUserRequest.getPhoneNumber();

    if (email != null && userRepository.existsByEmail(email)) {
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
      throw new AppException(ErrorCode.PHONE_EXISTED);
    }
  }

  private void validateUpdateUserRequest(UpdateUserRequest updateUserRequest, User existingUser) {
    String newUsername = updateUserRequest.getUsername();

    if (newUsername != null
        && !newUsername.trim().isEmpty()
        && !newUsername.equals(existingUser.getUsername())
        && userRepository.existsByUsername(newUsername)) {
      throw new AppException(ErrorCode.USERNAME_EXISTED);
    }
  }

  private void validateGetUserByIdRequest(Long id) {
    if (!hasRole(Role.USER.name())) {
      return;
    }

    Long currentUserId = getCurrentUserId();

    if (currentUserId.equals(id)) {
      return;
    }

    User currentUser = userHelper.findUserById(currentUserId);
    boolean isFriend = currentUser.getFriendList().stream()
        .anyMatch(friend -> friend.getId().equals(id));

    if (!isFriend) {
      throw new AppException(ErrorCode.USER_FORBIDDEN);
    }
  }

  private void validateUserAuthorization(Long id) {
    if (!hasRole(Role.USER.name())) {
      return;
    }

    Long currentUserId = getCurrentUserId();

    if (!currentUserId.equals(id)) {
      throw new AppException(ErrorCode.USER_FORBIDDEN);
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
