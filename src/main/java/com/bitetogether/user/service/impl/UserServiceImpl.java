package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;
import static com.bitetogether.common.util.SecurityUtils.hasRole;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.GlobalErrorCode;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserNotificationSettingsRequest;
import com.bitetogether.user.dto.user.request.UserOnlineStatus;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdItem;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserNotificationResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.enums.FriendRequestType;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.UserService;
import com.bitetogether.user.util.UserHelper;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
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
  FriendRequestServiceImpl friendRequestService;
  FriendRequestRepository friendRequestRepository;
  RefreshTokenRepository refreshTokenRepository;

  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,11}$");
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  @Override
  @Transactional
  public ApiResponse<Long> createUser(CreateUserRequest createUserRequest) {
    validateCreateUserRequest(createUserRequest);

    User newUser = userMapper.toEntity(createUserRequest);

    handlePassword(newUser);
    handleRole(newUser);

    User databaseUser = userHelper.saveUser(newUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User created successfully", databaseUser.getId());
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

  private void handlePassword(User newUser) {
    String encodedPassword = passwordEncoder.encode(newUser.getPassword());
    newUser.setPassword(encodedPassword);
  }

  private void handleRole(User newUser) {
    if (newUser.getRole() == null) {
      newUser.setRole(Role.USER.name());
    }
  }

  @Override
  @Transactional
  public ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest updateUserRequest) {
    User existingUser = userHelper.findUserById(id);

    validateUserAuthorization(id);

    validateUpdateUserRequest(updateUserRequest, existingUser);

    userMapper.updateUserFromRequest(updateUserRequest, existingUser);

    User updatedUser = userHelper.saveUser(existingUser);
    UserResponse userResponse = userMapper.toUserResponse(updatedUser);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User updated successfully", userResponse);
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

  @Override
  @Transactional
  public ApiResponse<String> deleteUser(Long id) {
    User existingUser = userHelper.findUserById(id);

    validateUserAuthorization(id);

    removeUserFromAllFriendships(existingUser);

    removeUserProperties(id);

    userRepository.delete(existingUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User with id " + id + " has been deleted successfully", null);
  }

  private void removeUserProperties(Long userId) {
    refreshTokenRepository.deleteAllByUserId(userId);
  }

  private void removeUserFromAllFriendships(User user) {
    friendRequestRepository.deleteAllByUserId(user.getId());

    Set<User> friendsCopy = new HashSet<>(user.getFriends());

    for (User friend : friendsCopy) {
      friend.getFriends().remove(user);
      userRepository.save(friend);
    }

    user.getFriends().clear();
  }

  @Override
  public ApiResponse<UserDetailsResponse> getCurrentUser() {
    Long currentUserId = getCurrentUserId();
    User currentUser = userHelper.findUserById(currentUserId);

    UserDetailsResponse userDetailsResponse = userMapper.toUserDetailsResponse(currentUser);
    userDetailsResponse.setFriendsCount(currentUser.getFriends().size());

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Your account's information has been fetched successfully",
        userDetailsResponse);
  }

  @Override
  public ApiResponse<UserGetByIdResponse> getUserById(Long id) {
    User user = userHelper.findUserById(id);

    UserGetByIdResponse userGetByIdResponse = userMapper.toUserGetByIdResponse(user);

    enrichWithFriendStatus(userGetByIdResponse, user);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "User's information has been fetched successfully",
        userGetByIdResponse);
  }

  private void enrichWithFriendStatus(UserGetByIdResponse response, User user) {
    Long currentUserId = getCurrentUserId();
    User currentUser = userHelper.findUserById(currentUserId);

    UserGetByIdItem friendItem = new UserGetByIdItem();

    if (currentUser.getFriends().contains(user)) {
      setFriendStatusFields(friendItem, user);
      response.setFriendItem(friendItem);
      return;
    }

    FriendRequestType friendRequestType =
        friendRequestService.getFriendRequestTypeBetweenUsers(currentUser, user);

    switch (friendRequestType) {
      case SENT -> setSentFriendRequestFields(friendItem, currentUser, user);
      case RECEIVED -> setReceivedFriendRequestFields(friendItem, currentUser, user);
      default -> setStrangerStatusFields(friendItem);
    }
    response.setFriendItem(friendItem);
  }

  private void setFriendStatusFields(UserGetByIdItem response, User user) {
    response.setIsFriend(true);
    response.setIsUserOnline(user.isOnline());
    response.setLastSeenUser(user.getLastSeen());
  }

  private void setSentFriendRequestFields(UserGetByIdItem response, User currentUser, User user) {
    response.setHasFriendRequestSent(true);
    response.setFriendRequestId(friendRequestService.getFriendRequestSentId(currentUser, user));
  }

  private void setReceivedFriendRequestFields(
      UserGetByIdItem response, User currentUser, User user) {
    response.setHasFriendRequestReceived(true);
    response.setFriendRequestId(friendRequestService.getFriendRequestReceivedId(currentUser, user));
  }

  private void setStrangerStatusFields(UserGetByIdItem response) {
    response.setIsFriend(false);
    response.setHasFriendRequestSent(false);
    response.setHasFriendRequestReceived(false);
  }

  @Override
  public ApiResponse<UserSearchResponse> searchUsersWithFilter(
      UserSearchRequest userSearchRequest) {
    String keyword = userSearchRequest.getKeyword().trim();

    User searchedUser = searchUser(keyword);

    UserSearchResponse userSearchResponse =
        searchedUser == null ? null : userMapper.toUserSearchResponse(searchedUser);
    String message =
        searchedUser == null
            ? "No users found matching the keyword"
            : "Users have been fetched successfully";

    return buildApiResponse(ApiResponseStatus.SUCCESS, message, userSearchResponse);
  }

  private User searchUser(String keyword) {
    if (PHONE_PATTERN.matcher(keyword).matches()) {
      return userRepository.findByPhoneNumber(keyword).orElse(null);
    } else if (EMAIL_PATTERN.matcher(keyword).matches()) {
      return userRepository.findByEmail(keyword).orElse(null);
    } else {
      throw new AppException(ErrorCode.INVALID_KEYWORD);
    }
  }

  @Override
  public ApiResponse<UserNotificationResponse> getNotificationSettings(Long id) {
    validateUserAuthorization(id);

    User user = userHelper.findUserById(id);

    UserNotificationResponse settingsRequest = userMapper.toUserNotificationResponse(user);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "User notification settings fetched successfully",
        settingsRequest);
  }

  @Override
  public ApiResponse<Void> updateNotificationSettings(
      Long id, UserNotificationSettingsRequest userNotificationSettingsRequest) {
    validateUserAuthorization(id);

    User user = userHelper.findUserById(id);

    userMapper.updateUserNotificationSettingsFromRequest(userNotificationSettingsRequest, user);

    userHelper.saveUser(user);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User notification settings updated successfully", null);
  }

  @Override
  public ApiResponse<Void> setUserOnline(Long id, UserOnlineStatus userOnlineStatus) {
    validateUserAuthorization(id);

    User user = userHelper.findUserById(id);

    user.setOnline(userOnlineStatus.isOnline());
    user.setLastSeen(LocalDateTime.now());

    userHelper.saveUser(user);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User has been updated successfully", null);
  }

  private void validateUserAuthorization(Long id) {
    if (!hasRole(Role.USER.name())) {
      return;
    }

    Long currentUserId = getCurrentUserId();

    if (!currentUserId.equals(id)) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }
  }
}
