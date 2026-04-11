package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.user.util.AuthUtils.getCurrentUserId;
import static com.bitetogether.user.util.AuthUtils.hasRole;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.GlobalErrorCode;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.event.UserCreatedEvent;
import com.bitetogether.user.dto.event.UserUpdatedEvent;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdatePhoneRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserState;
import com.bitetogether.user.dto.user.request.UserNotificationSettingsRequest;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.request.ValidateUserCriteriaRequest;
import com.bitetogether.user.dto.user.response.ListUserDetailsResponse;
import com.bitetogether.user.dto.user.response.UpdatePhoneResponse;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdItem;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserNotificationResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.dto.user.response.UserStateResponse;
import com.bitetogether.user.dto.user.response.ValidateUserCriteriaResponse;
import com.bitetogether.user.enums.FriendRequestType;
import com.bitetogether.user.enums.UserState;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.EventPublisherService;
import com.bitetogether.user.service.FirebaseAuthService;
import com.bitetogether.user.service.UserCacheService;
import com.bitetogether.user.service.UserService;
import com.bitetogether.user.util.UserHelper;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
  UserRepository userRepository;
  UserMapper userMapper;
  UserHelper userHelper;
  FriendRequestServiceImpl friendRequestService;
  FriendRequestRepository friendRequestRepository;
  RefreshTokenRepository refreshTokenRepository;
  EventPublisherService eventPublisherService;
  FirebaseStorageServiceImpl firebaseStorageService;
  FirebaseAuthService firebaseAuthService;
  UserCacheService userCacheService;

  // Validation constants
  private static final int USERNAME_MIN_LENGTH = 6;
  private static final int USERNAME_MAX_LENGTH = 20;
  private static final String USERNAME_PATTERN = "^[a-zA-Z0-9._]{6,20}$";
  private static final String PHONE_NUMBER_CLAIM = "phone_number";

  @Override
  @Transactional
  public ApiResponseDTO<Long> createUser(CreateUserRequest createUserRequest) {
    validateCreateUserRequest(createUserRequest);

    User newUser = userMapper.toEntity(createUserRequest);

    handleRole(newUser);

    User databaseUser = userHelper.saveUser(newUser);

    // Publish user created event to Kafka
    publishUserCreatedEvent(databaseUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User created successfully", databaseUser.getId());
  }

  private void validateCreateUserRequest(CreateUserRequest createUserRequest) {
    String username = createUserRequest.getUsername();
    String phoneNumber = createUserRequest.getPhoneNumber();

    if (username != null && isUsernameTaken(username)) {
      throw new AppException(ErrorCode.USERNAME_EXISTED);
    }

    if (phoneNumber != null && isPhoneTaken(phoneNumber)) {
      throw new AppException(ErrorCode.PHONE_EXISTED);
    }
  }

  private void handleRole(User newUser) {
    if (newUser.getRole() == null) {
      newUser.setRole(Role.USER.name());
    }
  }

  @Override
  @Transactional
  public ApiResponseDTO<UserResponse> updateUser(Long id, UpdateUserRequest updateUserRequest) {
    User existingUser = userHelper.findUserById(id);

    validateUserAuthorization(id);

    validateUpdateUserRequest(updateUserRequest, existingUser);

    userMapper.updateUserFromRequest(updateUserRequest, existingUser);

    User updatedUser = userHelper.saveUser(existingUser);
    UserResponse userResponse = userMapper.toUserResponse(updatedUser);

    // Publish user updated event to Kafka
    publishUserUpdatedEvent(updatedUser, updatedUser.getVersion());

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User updated successfully", userResponse);
  }

  private void validateUpdateUserRequest(UpdateUserRequest updateUserRequest, User existingUser) {
    String newUsername = updateUserRequest.getUsername();

    if (isUsernameChangeRequired(newUsername, existingUser)) {
      validateUsernameForUpdate(newUsername);
    }
  }

  private boolean isUsernameChangeRequired(String newUsername, User existingUser) {
    return newUsername != null
        && !newUsername.trim().isEmpty()
        && !newUsername.equals(existingUser.getUsername());
  }

  private void validateUsernameForUpdate(String username) {
    if (!isUsernameFormatValid(username)) {
      throw new AppException(ErrorCode.INVALID_USERNAME_FORMAT);
    }

    if (isUsernameTaken(username)) {
      throw new AppException(ErrorCode.USERNAME_EXISTED);
    }
  }

  @Override
  @Transactional
  public ApiResponseDTO<String> deleteUser(Long id) {
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
  public ApiResponseDTO<UserDetailsResponse> getCurrentUser() {
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
  public ApiResponseDTO<UserGetByIdResponse> getUserById(Long id) {
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
    response.setIsUserOnline(isUserOnline(user));
    response.setLastSeenUser(user.getLastSeen());
  }

  private boolean isUserOnline(User user) {
    return user.getState() != UserState.OFFLINE;
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
  public ApiResponseDTO<UserSearchResponse> searchUsersWithFilter(
      UserSearchRequest userSearchRequest) {
    String keyword = userSearchRequest.getKeyword().trim();

    User searchedUser = searchUser(keyword);

    // If the searched user is the current user, return empty result
    if (searchedUser != null) {
      Long currentUserId = getCurrentUserId();
      if (currentUserId != null && currentUserId.equals(searchedUser.getId())) {
        return buildApiResponse(
            ApiResponseStatus.SUCCESS, "No users found matching the keyword", null);
      }
    }

    UserSearchResponse userSearchResponse =
        searchedUser == null ? null : userMapper.toUserSearchResponse(searchedUser);

    // Enrich with friend status if user found
    if (userSearchResponse != null) {
      enrichSearchResultWithFriendStatus(userSearchResponse, searchedUser);
    }

    String message =
        searchedUser == null
            ? "No users found matching the keyword"
            : "Users have been searched successfully";

    return buildApiResponse(ApiResponseStatus.SUCCESS, message, userSearchResponse);
  }

  private void enrichSearchResultWithFriendStatus(UserSearchResponse response, User searchedUser) {
    Long currentUserId = getCurrentUserId();
    User currentUser = userHelper.findUserById(currentUserId);

    // Check if they are already friends
    if (currentUser.getFriends().contains(searchedUser)) {
      response.setIsFriend(true);
      return;
    }

    // Check friend request status
    FriendRequestType friendRequestType =
        friendRequestService.getFriendRequestTypeBetweenUsers(currentUser, searchedUser);

    switch (friendRequestType) {
      case SENT -> {
        response.setHasFriendRequestSent(true);
        response.setFriendRequestId(
            friendRequestService.getFriendRequestSentId(currentUser, searchedUser));
      }
      case RECEIVED -> {
        response.setHasFriendRequestReceived(true);
        response.setFriendRequestId(
            friendRequestService.getFriendRequestReceivedId(currentUser, searchedUser));
      }
      default -> {
        response.setIsFriend(false);
        response.setHasFriendRequestSent(false);
        response.setHasFriendRequestReceived(false);
      }
    }
  }

  private User searchUser(String keyword) {
    // Search by username only (not by phone or email for privacy)
    return userRepository.findByUsername(keyword).orElse(null);
  }

  @Override
  public ApiResponseDTO<UserNotificationResponse> getNotificationSettings(Long id) {
    validateUserAuthorization(id);

    User user = userHelper.findUserById(id);

    UserNotificationResponse settingsRequest = userMapper.toUserNotificationResponse(user);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "User notification settings fetched successfully",
        settingsRequest);
  }

  @Override
  public ApiResponseDTO<Void> updateNotificationSettings(
      Long id, UserNotificationSettingsRequest userNotificationSettingsRequest) {
    validateUserAuthorization(id);

    User user = userHelper.findUserById(id);

    user.setPushNotificationsEnabled(userNotificationSettingsRequest.getPushNotificationsEnabled());

    userHelper.saveUser(user);

    // Evict cache to ensure next read gets updated value
    userCacheService.evictUserState(id);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User notification settings updated successfully", null);
  }

  private void validateUserAuthorization(Long id) {
    if (!hasRole(Role.USER.name())) {
      return;
    }

    Long currentUserId = getCurrentUserId();

    if (currentUserId == null || !currentUserId.equals(id)) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }
  }

  @Override
  public ApiResponseDTO<ListUserDetailsResponse> getListUser(List<Long> userIds) {
    Long currentUserId = getCurrentUserId();

    if (!userIds.contains(currentUserId)) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }

    List<User> users = userRepository.findAllById(userIds);
    List<UserDetailsResponse> response =
        users.stream().map(userMapper::toUserDetailsResponse).toList();

    ListUserDetailsResponse listUserDetailsResponse = new ListUserDetailsResponse();
    listUserDetailsResponse.setUsers(response);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "List users have been fetched successfully",
        listUserDetailsResponse);
  }

  @Override
  @Transactional
  public ApiResponseDTO<String> uploadAvatar(Long userId, MultipartFile file) {
    Long currentUserId = getCurrentUserId();

    // Check if user is updating their own avatar or has admin role
    if (isUserAuthorizedForAction(currentUserId, userId)) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }

    // Find user
    User user = userHelper.findUserById(userId);

    // Delete old avatar if exists
    deleteOldAvatarIfExists(user);

    // Upload new avatar
    String avatarUrl = firebaseStorageService.uploadAvatar(file, userId);

    // Update user avatar
    user.setAvatar(avatarUrl);
    userRepository.save(user);

    log.info("Avatar uploaded successfully for user {}", userId);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Avatar uploaded successfully", avatarUrl);
  }

  @Override
  @Transactional
  public ApiResponseDTO<Void> deleteAvatar(Long userId) {
    Long currentUserId = getCurrentUserId();

    // Check if user is deleting their own avatar or has admin role
    if (isUserAuthorizedForAction(currentUserId, userId)) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }

    // Find user
    User user = userHelper.findUserById(userId);

    // Check if user has avatar
    validateUserHasAvatar(user);

    // Delete avatar from Firebase Storage
    deleteAvatarFromStorage(user);

    // Remove avatar URL from user
    user.setAvatar(null);
    userRepository.save(user);

    log.info("Avatar deleted successfully for user {}", userId);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Avatar deleted successfully", null);
  }

  @Override
  public ApiResponseDTO<ValidateUserCriteriaResponse> validateUserCriteria(
      ValidateUserCriteriaRequest criteria) {
    boolean isValid;
    String message;

    switch (criteria.getCriteriaType()) {
      case USERNAME -> {
        message = validateUsernameWithDetailedMessage(criteria.getCriteriaValue());
        isValid = message.contains("valid and available");
      }
      case PHONE -> {
        isValid = isPhoneAvailable(criteria.getCriteriaValue());
        message = buildPhoneValidationMessage(isValid);
      }
      default -> {
        isValid = false;
        message = "Invalid criteria type";
      }
    }

    ValidateUserCriteriaResponse response =
        ValidateUserCriteriaResponse.builder().isValid(isValid).validationMessage(message).build();

    return buildApiResponse(ApiResponseStatus.SUCCESS, message, response);
  }

  private String validateUsernameWithDetailedMessage(String username) {
    if (StringUtils.isEmpty(username)) {
      return "Username cannot be empty";
    }

    if (!isUsernameLengthValid(username)) {
      return String.format(
          "Username must be between %d and %d characters",
          USERNAME_MIN_LENGTH, USERNAME_MAX_LENGTH);
    }

    if (!isUsernamePatternValid(username)) {
      return "Username can only contain letters, numbers, dots (.), and underscores (_)";
    }

    if (isUsernameTaken(username)) {
      return "This username is already taken";
    }

    return "Username is valid and available";
  }

  private boolean isUsernameFormatValid(String username) {
    return !StringUtils.isEmpty(username)
        && isUsernameLengthValid(username)
        && isUsernamePatternValid(username);
  }

  private boolean isUsernameLengthValid(String username) {
    int length = username.length();
    return length >= USERNAME_MIN_LENGTH && length <= USERNAME_MAX_LENGTH;
  }

  private boolean isUsernamePatternValid(String username) {
    return username.matches(USERNAME_PATTERN);
  }

  private boolean isUsernameTaken(String username) {
    return userRepository.existsByUsername(username);
  }

  private boolean isPhoneAvailable(String phoneNumber) {
    return StringUtils.isNotEmpty(phoneNumber) && !isPhoneTaken(phoneNumber);
  }

  private boolean isPhoneTaken(String phoneNumber) {
    return userRepository.existsByPhoneNumber(phoneNumber);
  }

  private String buildPhoneValidationMessage(boolean isValid) {
    return isValid ? "Phone number is available" : "Phone number is already registered";
  }

  private boolean isUserAuthorizedForAction(Long currentUserId, Long targetUserId) {
    if (currentUserId == null) {
      return true;
    }
    return !currentUserId.equals(targetUserId) && !hasRole(Role.ADMIN.name());
  }

  private void deleteOldAvatarIfExists(User user) {
    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
      firebaseStorageService.deleteFile(user.getAvatar());
      log.info("Deleted old avatar for user {}", user.getId());
    }
  }

  private void validateUserHasAvatar(User user) {
    if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
      throw new AppException(ErrorCode.AVATAR_NOT_FOUND);
    }
  }

  private void deleteAvatarFromStorage(User user) {
    boolean deleted = firebaseStorageService.deleteFile(user.getAvatar());

    if (!deleted) {
      log.warn("Failed to delete avatar from Firebase Storage for user {}", user.getId());
    }
  }

  @Override
  @Transactional
  public ApiResponseDTO<UpdatePhoneResponse> updatePhone(UpdatePhoneRequest updatePhoneRequest) {
    // Get current authenticated user ID
    Long userId = getCurrentUserId();

    // Verify Firebase ID Token
    FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(updatePhoneRequest.getIdToken());

    String newFirebaseUid = decodedToken.getUid();
    String newPhoneNumber = (String) decodedToken.getClaims().get(PHONE_NUMBER_CLAIM);

    log.info(
        "Update phone request - User ID: {}, New UID: {}, New Phone: {}",
        userId,
        newFirebaseUid,
        newPhoneNumber);

    // Validate phone number from token
    if (StringUtils.isEmpty(newPhoneNumber)) {
      throw new AppException(ErrorCode.INVALID_FIREBASE_TOKEN);
    }

    // Get current user
    User user = userHelper.findUserById(userId);

    // Check if new phone is already used by another user
    validatePhoneNotUsedByOtherUser(newPhoneNumber, userId);

    // Update user's phone and Firebase UID
    user.setPhoneNumber(newPhoneNumber);
    user.setFirebaseUid(newFirebaseUid);

    User updatedUser = userRepository.save(user);

    log.info("Phone updated successfully for user {}: {}", userId, newPhoneNumber);

    UpdatePhoneResponse response =
        UpdatePhoneResponse.builder()
            .userId(updatedUser.getId())
            .phoneNumber(updatedUser.getPhoneNumber())
            .firebaseUid(updatedUser.getFirebaseUid())
            .build();

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "Phone number updated successfully", response);
  }

  private void validatePhoneNotUsedByOtherUser(String phoneNumber, Long currentUserId) {
    userRepository
        .findByPhoneNumber(phoneNumber)
        .ifPresent(
            existingUser -> {
              if (!existingUser.getId().equals(currentUserId)) {
                throw new AppException(ErrorCode.PHONE_EXISTED);
              }
            });
  }

  // ==================== KAFKA EVENT PUBLISHERS ====================

  private void publishUserCreatedEvent(User user) {
    UserCreatedEvent event =
        UserCreatedEvent.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .avatar(user.getAvatar())
            .eventTimestamp(LocalDateTime.now())
            .version(0L) // Initial version
            .build();

    eventPublisherService.publishUserCreatedEvent(event);
  }

  private void publishUserUpdatedEvent(User user, Long version) {
    UserUpdatedEvent event =
        UserUpdatedEvent.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .avatar(user.getAvatar())
            .eventTimestamp(LocalDateTime.now())
            .version(version)
            .build();

    eventPublisherService.publishUserUpdatedEvent(event);
  }

  @Override
  public ApiResponseDTO<Void> updateUserState(Long id, UpdateUserState updateUserState) {
    validateUserAuthorization(id);

    User user = userHelper.findUserById(id);
    UserState newState = updateUserState.getState();

    user.setState(newState);

    // Only update lastSeen when user goes OFFLINE or BACKGROUND
    if (newState == UserState.OFFLINE || newState == UserState.BACKGROUND) {
      user.setLastSeen(LocalDateTime.now());
    }

    userHelper.saveUser(user);

    userCacheService.cacheUserState(
        id, user.getState(), user.getLastSeen(), user.isPushNotificationsEnabled());

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User has been updated successfully", null);
  }

  @Override
  public ApiResponseDTO<UserStateResponse> getCurrentUserState() {
    Long currentUserId = getCurrentUserId();

    // Try to get from cache first
    UserStateResponse cachedState = userCacheService.getCachedUserState(currentUserId).orElse(null);

    if (cachedState != null) {
      log.debug("Retrieved user state from cache for user ID: {}", currentUserId);
      return buildApiResponse(
          ApiResponseStatus.SUCCESS, "User state fetched successfully", cachedState);
    }

    // Cache miss - fallback to database
    log.debug(
        "User state not found in cache, fetching from database for user ID: {}", currentUserId);
    User user = userHelper.findUserById(currentUserId);

    UserStateResponse response =
        UserStateResponse.builder()
            .state(user.getState())
            .lastSeen(user.getLastSeen())
            .pushNotificationsEnabled(user.isPushNotificationsEnabled())
            .build();

    // Cache the result for next time
    userCacheService.cacheUserState(
        currentUserId, user.getState(), user.getLastSeen(), user.isPushNotificationsEnabled());

    return buildApiResponse(ApiResponseStatus.SUCCESS, "User state fetched successfully", response);
  }
}
