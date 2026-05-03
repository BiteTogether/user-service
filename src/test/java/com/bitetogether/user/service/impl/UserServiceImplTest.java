package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.event.UserDeletedEvent;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdatePhoneRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.request.ValidateUserCriteriaRequest;
import com.bitetogether.user.dto.user.response.ListUserDetailsResponse;
import com.bitetogether.user.dto.user.response.UpdatePhoneResponse;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.dto.user.response.ValidateUserCriteriaResponse;
import com.bitetogether.user.enums.FriendRequestType;
import com.bitetogether.user.enums.ValidateCriteria;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.ConversationService;
import com.bitetogether.user.service.EventPublisherService;
import com.bitetogether.user.service.FirebaseAuthService;
import com.bitetogether.user.util.AuthUtils;
import com.bitetogether.user.util.UserHelper;
import com.google.firebase.auth.FirebaseToken;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
  "java:S2699",
  "java:S6073"
}) // Sonar: assertions present, unboxing warnings are false positives
class UserServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private UserMapper userMapper;

  @Mock private UserHelper userHelper;

  @Mock private FriendRequestServiceImpl friendRequestService;

  @Mock private FriendRequestRepository friendRequestRepository;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private EventPublisherService eventPublisherService;

  @Mock private FirebaseStorageServiceImpl firebaseStorageService;

  @Mock private FirebaseAuthService firebaseAuthService;

  @Mock private ConversationService conversationService;

  @InjectMocks private UserServiceImpl userService;

  private User testUser;
  private User friendUser;
  private CreateUserRequest createUserRequest;
  private UpdateUserRequest updateUserRequest;
  private UserSearchRequest userSearchRequest;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .firebaseUid("firebase-uid-1")
            .fullName("Test User")
            .phoneNumber("1234567890")
            .role(Role.USER.name())
            .friends(new HashSet<>())
            .lastSeen(LocalDateTime.now())
            .build();

    friendUser =
        User.builder()
            .id(2L)
            .username("frienduser")
            .firebaseUid("firebase-uid-2")
            .fullName("Friend User")
            .phoneNumber("0987654321")
            .role(Role.USER.name())
            .friends(new HashSet<>())
            .lastSeen(LocalDateTime.now())
            .build();

    createUserRequest = new CreateUserRequest();
    createUserRequest.setUsername("newuser");
    createUserRequest.setFirebaseUid("firebase-uid-new");
    createUserRequest.setFullName("New User");
    createUserRequest.setPhoneNumber("1112223333");

    updateUserRequest = new UpdateUserRequest();
    updateUserRequest.setUsername("updateduser");
    updateUserRequest.setFullName("Updated Name");

    userSearchRequest = new UserSearchRequest();
    userSearchRequest.setKeyword("testuser");
  }

  // ==================== createUser tests ====================

  @Test
  void createUser_WithValidRequest_ReturnsUserId() {
    User newUser = User.builder().id(null).build();
    User savedUser = User.builder().id(3L).build();

    when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
    when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(false);
    when(userMapper.toEntity(createUserRequest)).thenReturn(newUser);
    when(userHelper.saveUser(any(User.class))).thenReturn(savedUser);

    ApiResponseDTO<Long> response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    assertEquals("User created successfully", response.getMessage());
    assertEquals(3L, response.getData());

    verify(userRepository, times(1)).existsByUsername(createUserRequest.getUsername());
    verify(userRepository, times(1)).existsByPhoneNumber(createUserRequest.getPhoneNumber());
    verify(userHelper, times(1)).saveUser(any(User.class));
  }

  @Test
  void createUser_WithExistingUsername_ThrowsException() {
    when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(true);

    assertThrows(AppException.class, () -> userService.createUser(createUserRequest));

    verify(userRepository, times(1)).existsByUsername(createUserRequest.getUsername());
    verify(userRepository, never()).existsByPhoneNumber(any());
    verify(userHelper, never()).saveUser(any());
  }

  @Test
  void createUser_WithExistingPhoneNumber_ThrowsException() {
    when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
    when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(true);

    assertThrows(AppException.class, () -> userService.createUser(createUserRequest));

    verify(userRepository, times(1)).existsByUsername(createUserRequest.getUsername());
    verify(userRepository, times(1)).existsByPhoneNumber(createUserRequest.getPhoneNumber());
    verify(userHelper, never()).saveUser(any());
  }

  @Test
  void createUser_WithNullRole_SetsDefaultRole() {
    User newUser = User.builder().id(null).role(null).build();
    User savedUser = User.builder().id(3L).role(Role.USER.name()).build();

    when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
    when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(false);
    when(userMapper.toEntity(createUserRequest)).thenReturn(newUser);
    when(userHelper.saveUser(any(User.class))).thenReturn(savedUser);

    ApiResponseDTO<Long> response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    verify(userHelper, times(1)).saveUser(any(User.class));
  }

  @Test
  void createUser_WithNullUsername_SkipsUsernameValidation() {
    createUserRequest.setUsername(null);
    User newUser = User.builder().id(null).build();
    User savedUser = User.builder().id(3L).build();

    when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(false);
    when(userMapper.toEntity(createUserRequest)).thenReturn(newUser);
    when(userHelper.saveUser(any(User.class))).thenReturn(savedUser);

    ApiResponseDTO<Long> response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    verify(userRepository, never()).existsByUsername(anyString());
  }

  @Test
  void createUser_WithNullPhoneNumber_SkipsPhoneValidation() {
    createUserRequest.setPhoneNumber(null);
    User newUser = User.builder().id(null).build();
    User savedUser = User.builder().id(3L).build();

    when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
    when(userMapper.toEntity(createUserRequest)).thenReturn(newUser);
    when(userHelper.saveUser(any(User.class))).thenReturn(savedUser);

    ApiResponseDTO<Long> response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    verify(userRepository, never()).existsByPhoneNumber(anyString());
  }

  @Test
  void createUser_WithExistingRole_KeepsExistingRole() {
    User newUser = User.builder().id(null).role(Role.ADMIN.name()).build();
    User savedUser = User.builder().id(3L).role(Role.ADMIN.name()).build();

    when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
    when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(false);
    when(userMapper.toEntity(createUserRequest)).thenReturn(newUser);
    when(userHelper.saveUser(any(User.class))).thenReturn(savedUser);

    userService.createUser(createUserRequest);

    assertEquals(Role.ADMIN.name(), newUser.getRole());
  }

  // ==================== updateUser tests ====================

  @Test
  void updateUser_WithValidRequest_ReturnsUserResponse() {
    Long userId = 1L;
    UserResponse userResponse = UserResponse.builder().id(userId).username("updateduser").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userRepository.existsByUsername(updateUserRequest.getUsername())).thenReturn(false);
      when(userHelper.saveUser(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

      ApiResponseDTO<UserResponse> response = userService.updateUser(userId, updateUserRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("User updated successfully", response.getMessage());
      assertEquals(userResponse, response.getData());

      verify(userHelper, times(1)).findUserById(userId);
      verify(userMapper, times(1)).updateUserFromRequest(updateUserRequest, testUser);
      verify(userHelper, times(1)).saveUser(testUser);
    }
  }

  @Test
  void updateUser_WithExistingUsername_ThrowsException() {
    Long userId = 1L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userRepository.existsByUsername(updateUserRequest.getUsername())).thenReturn(true);

      assertThrows(AppException.class, () -> userService.updateUser(userId, updateUserRequest));

      verify(userHelper, never()).saveUser(any());
    }
  }

  @Test
  void updateUser_WithUnauthorizedUser_ThrowsException() {
    Long userId = 1L;
    Long differentUserId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(differentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.updateUser(userId, updateUserRequest));

      verify(userHelper, never()).saveUser(any());
    }
  }

  @Test
  void updateUser_WithSameUsername_SkipsUsernameValidation() {
    Long userId = 1L;
    updateUserRequest.setUsername("testuser"); // same as existing
    UserResponse userResponse = UserResponse.builder().id(userId).username("testuser").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userHelper.saveUser(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

      ApiResponseDTO<UserResponse> response = userService.updateUser(userId, updateUserRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      verify(userRepository, never()).existsByUsername(anyString());
    }
  }

  @Test
  void updateUser_WithNullUsername_SkipsUsernameValidation() {
    Long userId = 1L;
    updateUserRequest.setUsername(null);
    UserResponse userResponse = UserResponse.builder().id(userId).build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userHelper.saveUser(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

      ApiResponseDTO<UserResponse> response = userService.updateUser(userId, updateUserRequest);

      assertNotNull(response);
      verify(userRepository, never()).existsByUsername(anyString());
    }
  }

  @Test
  void updateUser_WithEmptyUsername_SkipsUsernameValidation() {
    Long userId = 1L;
    updateUserRequest.setUsername("   ");
    UserResponse userResponse = UserResponse.builder().id(userId).build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userHelper.saveUser(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

      ApiResponseDTO<UserResponse> response = userService.updateUser(userId, updateUserRequest);

      assertNotNull(response);
      verify(userRepository, never()).existsByUsername(anyString());
    }
  }

  @Test
  void updateUser_WithInvalidUsernameFormat_ThrowsException() {
    Long userId = 1L;
    updateUserRequest.setUsername("ab"); // too short

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.updateUser(userId, updateUserRequest));
      verify(userHelper, never()).saveUser(any());
    }
  }

  @Test
  void updateUser_WithInvalidUsernameChars_ThrowsException() {
    Long userId = 1L;
    updateUserRequest.setUsername("user@name!"); // invalid chars

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.updateUser(userId, updateUserRequest));
      verify(userHelper, never()).saveUser(any());
    }
  }

  @Test
  void updateUser_AsNonUserRole_SkipsAuthorizationCheck() {
    Long userId = 1L;
    Long differentUserId = 2L;
    UserResponse userResponse = UserResponse.builder().id(userId).build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(differentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userRepository.existsByUsername(updateUserRequest.getUsername())).thenReturn(false);
      when(userHelper.saveUser(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

      ApiResponseDTO<UserResponse> response = userService.updateUser(userId, updateUserRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    }
  }

  // ==================== deleteUser tests ====================

  @Test
  void deleteUser_WithValidUser_DeletesSuccessfully() {
    Long userId = 1L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      ApiResponseDTO<String> response = userService.deleteUser(userId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertTrue(response.getMessage().contains("has been deleted successfully"));

      verify(friendRequestRepository, times(1)).deleteAllByUserId(userId);
      verify(refreshTokenRepository, times(1)).deleteAllByUserId(userId);
      verify(userRepository, times(1)).delete(testUser);
      verify(eventPublisherService, times(1)).publishUserDeletedEvent(any(UserDeletedEvent.class));
    }
  }

  @Test
  void deleteUser_WithFriends_RemovesFriendships() {
    Long userId = 1L;
    testUser.getFriends().add(friendUser);
    friendUser.getFriends().add(testUser);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      ApiResponseDTO<String> response = userService.deleteUser(userId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());

      verify(userRepository, times(1)).save(friendUser);
      verify(userRepository, times(1)).delete(testUser);
      verify(eventPublisherService, times(1)).publishUserDeletedEvent(any(UserDeletedEvent.class));
      assertTrue(testUser.getFriends().isEmpty());
    }
  }

  @Test
  void deleteUser_WithUnauthorizedUser_ThrowsException() {
    Long userId = 1L;
    Long differentUserId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(differentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.deleteUser(userId));
      verify(userRepository, never()).delete(any());
    }
  }

  @Test
  void deleteUser_WithNullCurrentUserId_ThrowsException() {
    Long userId = 1L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(null);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.deleteUser(userId));
      verify(userRepository, never()).delete(any());
    }
  }

  // ==================== getCurrentUser tests ====================

  @Test
  void getCurrentUser_WithValidUser_ReturnsUserDetails() {
    Long currentUserId = 1L;
    UserDetailsResponse userDetailsResponse =
        UserDetailsResponse.builder()
            .id(currentUserId)
            .username("testuser")
            .friendsCount(0)
            .build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserDetailsResponse(testUser)).thenReturn(userDetailsResponse);

      ApiResponseDTO<UserDetailsResponse> response = userService.getCurrentUser();

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals(
          "Your account's information has been fetched successfully", response.getMessage());
      assertEquals(userDetailsResponse, response.getData());
      assertEquals(0, response.getData().getFriendsCount());

      verify(userHelper, times(1)).findUserById(currentUserId);
    }
  }

  @Test
  void getCurrentUser_WithFriends_ReturnsFriendsCount() {
    Long currentUserId = 1L;
    testUser.getFriends().add(friendUser);
    UserDetailsResponse userDetailsResponse =
        UserDetailsResponse.builder().id(currentUserId).username("testuser").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserDetailsResponse(testUser)).thenReturn(userDetailsResponse);

      ApiResponseDTO<UserDetailsResponse> response = userService.getCurrentUser();

      assertNotNull(response);
      assertEquals(1, response.getData().getFriendsCount());
    }
  }

  // ==================== getUserById tests ====================

  @Test
  void getUserById_WhenUserIsFriend_ReturnsWithFriendStatus() {
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    UserGetByIdResponse userGetByIdResponse = new UserGetByIdResponse();
    userGetByIdResponse.setId(targetUserId);

    testUser.getFriends().add(friendUser);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userHelper.findUserById(targetUserId)).thenReturn(friendUser);
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserGetByIdResponse(friendUser)).thenReturn(userGetByIdResponse);
      when(conversationService.getDirectConversationId(targetUserId)).thenReturn("conv-2");

      ApiResponseDTO<UserGetByIdResponse> response = userService.getUserById(targetUserId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertNotNull(response.getData().getFriendItem());
      assertTrue(response.getData().getFriendItem().getIsFriend());
      assertEquals("conv-2", response.getData().getConversationId());
    }
  }

  @Test
  void getUserById_WhenFriendRequestSent_ReturnsWithSentStatus() {
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    Long requestId = 100L;
    UserGetByIdResponse userGetByIdResponse = new UserGetByIdResponse();
    userGetByIdResponse.setId(targetUserId);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userHelper.findUserById(targetUserId)).thenReturn(friendUser);
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserGetByIdResponse(friendUser)).thenReturn(userGetByIdResponse);
      when(friendRequestService.getFriendRequestTypeBetweenUsers(testUser, friendUser))
          .thenReturn(FriendRequestType.SENT);
      when(friendRequestService.getFriendRequestSentId(testUser, friendUser)).thenReturn(requestId);

      ApiResponseDTO<UserGetByIdResponse> response = userService.getUserById(targetUserId);

      assertNotNull(response);
      assertNotNull(response.getData().getFriendItem());
      assertTrue(response.getData().getFriendItem().getHasFriendRequestSent());
      assertEquals(requestId, response.getData().getFriendItem().getFriendRequestId());
    }
  }

  @Test
  void getUserById_WhenFriendRequestReceived_ReturnsWithReceivedStatus() {
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    Long requestId = 100L;
    UserGetByIdResponse userGetByIdResponse = new UserGetByIdResponse();
    userGetByIdResponse.setId(targetUserId);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userHelper.findUserById(targetUserId)).thenReturn(friendUser);
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserGetByIdResponse(friendUser)).thenReturn(userGetByIdResponse);
      when(friendRequestService.getFriendRequestTypeBetweenUsers(testUser, friendUser))
          .thenReturn(FriendRequestType.RECEIVED);
      when(friendRequestService.getFriendRequestReceivedId(testUser, friendUser))
          .thenReturn(requestId);

      ApiResponseDTO<UserGetByIdResponse> response = userService.getUserById(targetUserId);

      assertNotNull(response);
      assertNotNull(response.getData().getFriendItem());
      assertTrue(response.getData().getFriendItem().getHasFriendRequestReceived());
      assertEquals(requestId, response.getData().getFriendItem().getFriendRequestId());
    }
  }

  @Test
  void getUserById_WhenStranger_ReturnsWithStrangerStatus() {
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    UserGetByIdResponse userGetByIdResponse = new UserGetByIdResponse();
    userGetByIdResponse.setId(targetUserId);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userHelper.findUserById(targetUserId)).thenReturn(friendUser);
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserGetByIdResponse(friendUser)).thenReturn(userGetByIdResponse);
      when(friendRequestService.getFriendRequestTypeBetweenUsers(testUser, friendUser))
          .thenReturn(FriendRequestType.NONE);

      ApiResponseDTO<UserGetByIdResponse> response = userService.getUserById(targetUserId);

      assertNotNull(response);
      assertNotNull(response.getData().getFriendItem());
      assertFalse(response.getData().getFriendItem().getIsFriend());
      assertFalse(response.getData().getFriendItem().getHasFriendRequestSent());
      assertFalse(response.getData().getFriendItem().getHasFriendRequestReceived());
    }
  }

  // ==================== searchUsersWithFilter tests ====================

  @Test
  void searchUsersWithFilter_WithValidUsername_ReturnsUser() {
    Long currentUserId = 1L;
    Long searchedUserId = 2L;
    userSearchRequest.setKeyword("frienduser");
    UserSearchResponse userSearchResponse = new UserSearchResponse();
    userSearchResponse.setId(searchedUserId);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findByUsername("frienduser")).thenReturn(Optional.of(friendUser));
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserSearchResponse(friendUser)).thenReturn(userSearchResponse);
      when(friendRequestService.getFriendRequestTypeBetweenUsers(testUser, friendUser))
          .thenReturn(FriendRequestType.NONE);

      ApiResponseDTO<UserSearchResponse> response =
          userService.searchUsersWithFilter(userSearchRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertNotNull(response.getData());
      assertNull(response.getData().getConversationId());

      verify(userRepository, times(1)).findByUsername("frienduser");
    }
  }

  @Test
  void searchUsersWithFilter_WhenSearchedUserIsFriend_ReturnsConversationId() {
    Long currentUserId = 1L;
    Long searchedUserId = 2L;
    userSearchRequest.setKeyword("frienduser");
    UserSearchResponse userSearchResponse = new UserSearchResponse();
    userSearchResponse.setId(searchedUserId);
    testUser.getFriends().add(friendUser);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findByUsername("frienduser")).thenReturn(Optional.of(friendUser));
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserSearchResponse(friendUser)).thenReturn(userSearchResponse);
      when(conversationService.getDirectConversationId(searchedUserId)).thenReturn("conv-2");

      ApiResponseDTO<UserSearchResponse> response =
          userService.searchUsersWithFilter(userSearchRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertNotNull(response.getData());
      assertTrue(response.getData().getIsFriend());
      assertEquals("conv-2", response.getData().getConversationId());

      verify(userRepository, times(1)).findByUsername("frienduser");
    }
  }

  @Test
  void searchUsersWithFilter_SearchingSelf_ReturnsNull() {
    Long currentUserId = 1L;
    userSearchRequest.setKeyword("testuser");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

      ApiResponseDTO<UserSearchResponse> response =
          userService.searchUsersWithFilter(userSearchRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("No users found matching the keyword", response.getMessage());
      assertNull(response.getData());

      verify(userRepository, times(1)).findByUsername("testuser");
    }
  }

  @Test
  void searchUsersWithFilter_WithNoUserFound_ReturnsNullData() {
    Long currentUserId = 1L;
    userSearchRequest.setKeyword("nonexistent");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      ApiResponseDTO<UserSearchResponse> response =
          userService.searchUsersWithFilter(userSearchRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("No users found matching the keyword", response.getMessage());
      assertNull(response.getData());
    }
  }

  @Test
  void searchUsersWithFilter_WithSentFriendRequest_ReturnsSentStatus() {
    Long currentUserId = 1L;
    Long searchedUserId = 2L;
    userSearchRequest.setKeyword("frienduser");
    UserSearchResponse userSearchResponse = new UserSearchResponse();
    userSearchResponse.setId(searchedUserId);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findByUsername("frienduser")).thenReturn(Optional.of(friendUser));
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserSearchResponse(friendUser)).thenReturn(userSearchResponse);
      when(friendRequestService.getFriendRequestTypeBetweenUsers(testUser, friendUser))
          .thenReturn(FriendRequestType.SENT);
      when(friendRequestService.getFriendRequestSentId(testUser, friendUser)).thenReturn(100L);

      ApiResponseDTO<UserSearchResponse> response =
          userService.searchUsersWithFilter(userSearchRequest);

      assertNotNull(response);
      assertTrue(response.getData().getHasFriendRequestSent());
      assertEquals(100L, response.getData().getFriendRequestId());
    }
  }

  @Test
  void searchUsersWithFilter_WithReceivedFriendRequest_ReturnsReceivedStatus() {
    Long currentUserId = 1L;
    Long searchedUserId = 2L;
    userSearchRequest.setKeyword("frienduser");
    UserSearchResponse userSearchResponse = new UserSearchResponse();
    userSearchResponse.setId(searchedUserId);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findByUsername("frienduser")).thenReturn(Optional.of(friendUser));
      when(userHelper.findUserById(currentUserId)).thenReturn(testUser);
      when(userMapper.toUserSearchResponse(friendUser)).thenReturn(userSearchResponse);
      when(friendRequestService.getFriendRequestTypeBetweenUsers(testUser, friendUser))
          .thenReturn(FriendRequestType.RECEIVED);
      when(friendRequestService.getFriendRequestReceivedId(testUser, friendUser)).thenReturn(200L);

      ApiResponseDTO<UserSearchResponse> response =
          userService.searchUsersWithFilter(userSearchRequest);

      assertNotNull(response);
      assertTrue(response.getData().getHasFriendRequestReceived());
      assertEquals(200L, response.getData().getFriendRequestId());
    }
  }

  // ==================== getListUser tests ====================

  @Test
  void getListUser_WithValidRequest_ReturnsUserList() {
    Long currentUserId = 1L;
    List<Long> userIds = Arrays.asList(1L, 2L);
    List<User> users = Arrays.asList(testUser, friendUser);
    UserDetailsResponse userDetails1 =
        UserDetailsResponse.builder().id(1L).username("testuser").build();
    UserDetailsResponse userDetails2 =
        UserDetailsResponse.builder().id(2L).username("frienduser").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.findAllById(userIds)).thenReturn(users);
      when(userMapper.toUserDetailsResponse(testUser)).thenReturn(userDetails1);
      when(userMapper.toUserDetailsResponse(friendUser)).thenReturn(userDetails2);

      ApiResponseDTO<ListUserDetailsResponse> response = userService.getListUser(userIds);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("List users have been fetched successfully", response.getMessage());
      assertNotNull(response.getData());
      assertEquals(2, response.getData().getUsers().size());

      verify(userRepository, times(1)).findAllById(userIds);
    }
  }

  @Test
  void getListUser_WithoutCurrentUser_ThrowsException() {
    Long currentUserId = 3L;
    List<Long> userIds = Arrays.asList(1L, 2L);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      assertThrows(AppException.class, () -> userService.getListUser(userIds));

      verify(userRepository, never()).findAllById(anyList());
    }
  }

  // ==================== uploadAvatar tests ====================

  @Test
  void uploadAvatar_WithValidFile_ReturnsAvatarUrl() {
    Long userId = 1L;
    String avatarUrl = "https://storage.example.com/avatar.jpg";
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(firebaseStorageService.uploadAvatar(file, userId)).thenReturn(avatarUrl);
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<String> response = userService.uploadAvatar(userId, file);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Avatar uploaded successfully", response.getMessage());
      assertEquals(avatarUrl, response.getData());
      assertEquals(avatarUrl, testUser.getAvatar());
    }
  }

  @Test
  void uploadAvatar_WithExistingAvatar_DeletesOldAvatar() {
    Long userId = 1L;
    String oldAvatar = "https://storage.example.com/old-avatar.jpg";
    String newAvatar = "https://storage.example.com/new-avatar.jpg";
    testUser.setAvatar(oldAvatar);
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(firebaseStorageService.uploadAvatar(file, userId)).thenReturn(newAvatar);
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<String> response = userService.uploadAvatar(userId, file);

      assertNotNull(response);
      verify(firebaseStorageService, times(1)).deleteFile(oldAvatar);
      assertEquals(newAvatar, testUser.getAvatar());
    }
  }

  @Test
  void uploadAvatar_WithUnauthorizedUser_ThrowsException() {
    Long userId = 1L;
    Long differentUserId = 2L;
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(differentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      assertThrows(AppException.class, () -> userService.uploadAvatar(userId, file));
      verify(firebaseStorageService, never()).uploadAvatar(any(), anyLong());
    }
  }

  @Test
  void uploadAvatar_AsAdmin_AllowsCrossUserUpload() {
    Long userId = 1L;
    Long adminUserId = 99L;
    String avatarUrl = "https://storage.example.com/avatar.jpg";
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(adminUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(true);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(firebaseStorageService.uploadAvatar(file, userId)).thenReturn(avatarUrl);
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<String> response = userService.uploadAvatar(userId, file);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    }
  }

  @Test
  void uploadAvatar_WithNullCurrentUserId_ThrowsException() {
    Long userId = 1L;
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(null);

      assertThrows(AppException.class, () -> userService.uploadAvatar(userId, file));
    }
  }

  // ==================== deleteAvatar tests ====================

  @Test
  void deleteAvatar_WithExistingAvatar_DeletesSuccessfully() {
    Long userId = 1L;
    testUser.setAvatar("https://storage.example.com/avatar.jpg");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(firebaseStorageService.deleteFile(testUser.getAvatar())).thenReturn(true);
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<Void> response = userService.deleteAvatar(userId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Avatar deleted successfully", response.getMessage());
      assertNull(testUser.getAvatar());
    }
  }

  @Test
  void deleteAvatar_WithNoAvatar_ThrowsException() {
    Long userId = 1L;
    testUser.setAvatar(null);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.deleteAvatar(userId));
    }
  }

  @Test
  void deleteAvatar_WithEmptyAvatar_ThrowsException() {
    Long userId = 1L;
    testUser.setAvatar("");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);

      assertThrows(AppException.class, () -> userService.deleteAvatar(userId));
    }
  }

  @Test
  void deleteAvatar_WithUnauthorizedUser_ThrowsException() {
    Long userId = 1L;
    Long differentUserId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(differentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      assertThrows(AppException.class, () -> userService.deleteAvatar(userId));
      verify(firebaseStorageService, never()).deleteFile(anyString());
    }
  }

  @Test
  void deleteAvatar_WhenFirebaseDeleteFails_StillClearsAvatar() {
    Long userId = 1L;
    testUser.setAvatar("https://storage.example.com/avatar.jpg");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.ADMIN.name())).thenReturn(false);

      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(firebaseStorageService.deleteFile(testUser.getAvatar())).thenReturn(false);
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<Void> response = userService.deleteAvatar(userId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertNull(testUser.getAvatar());
    }
  }

  // ==================== validateUserCriteria tests ====================

  @Test
  void validateUserCriteria_WithValidAvailableUsername_ReturnsValid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("validuser123");

    when(userRepository.existsByUsername("validuser123")).thenReturn(false);

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertTrue(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("valid and available"));
  }

  @Test
  void validateUserCriteria_WithTakenUsername_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("takenuser1");

    when(userRepository.existsByUsername("takenuser1")).thenReturn(true);

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("already taken"));
  }

  @Test
  void validateUserCriteria_WithEmptyUsername_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("");

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("cannot be empty"));
  }

  @Test
  void validateUserCriteria_WithTooShortUsername_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("ab");

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("between"));
  }

  @Test
  void validateUserCriteria_WithTooLongUsername_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("a".repeat(21));

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
  }

  @Test
  void validateUserCriteria_WithInvalidUsernamePattern_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("user@name!");

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("only contain"));
  }

  @Test
  void validateUserCriteria_WithAvailablePhone_ReturnsValid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.PHONE);
    request.setCriteriaValue("1234567890");

    when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertTrue(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("available"));
  }

  @Test
  void validateUserCriteria_WithTakenPhone_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.PHONE);
    request.setCriteriaValue("1234567890");

    when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(true);

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
    assertTrue(response.getData().getValidationMessage().contains("already registered"));
  }

  @Test
  void validateUserCriteria_WithEmptyPhone_ReturnsInvalid() {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.PHONE);
    request.setCriteriaValue("");

    ApiResponseDTO<ValidateUserCriteriaResponse> response =
        userService.validateUserCriteria(request);

    assertNotNull(response);
    assertFalse(response.getData().isValid());
  }

  // ==================== updatePhone tests ====================

  @Test
  void updatePhone_WithValidRequest_ReturnsUpdatePhoneResponse() {
    Long userId = 1L;
    String newPhone = "+84123456789";
    String newUid = "new-firebase-uid";
    UpdatePhoneRequest request = new UpdatePhoneRequest();
    request.setIdToken("valid-id-token");

    FirebaseToken firebaseToken = org.mockito.Mockito.mock(FirebaseToken.class);
    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", newPhone);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      when(firebaseAuthService.verifyIdToken("valid-id-token")).thenReturn(firebaseToken);
      when(firebaseToken.getUid()).thenReturn(newUid);
      when(firebaseToken.getClaims()).thenReturn(claims);
      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userRepository.findByPhoneNumber(newPhone)).thenReturn(Optional.empty());
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<UpdatePhoneResponse> response = userService.updatePhone(request);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Phone number updated successfully", response.getMessage());
      assertEquals(newPhone, testUser.getPhoneNumber());
      assertEquals(newUid, testUser.getFirebaseUid());
    }
  }

  @Test
  void updatePhone_WithEmptyPhoneInToken_ThrowsException() {
    Long userId = 1L;
    UpdatePhoneRequest request = new UpdatePhoneRequest();
    request.setIdToken("valid-id-token");

    FirebaseToken firebaseToken = org.mockito.Mockito.mock(FirebaseToken.class);
    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", "");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      when(firebaseAuthService.verifyIdToken("valid-id-token")).thenReturn(firebaseToken);
      when(firebaseToken.getUid()).thenReturn("uid");
      when(firebaseToken.getClaims()).thenReturn(claims);

      assertThrows(AppException.class, () -> userService.updatePhone(request));
    }
  }

  @Test
  void updatePhone_WithNullPhoneInToken_ThrowsException() {
    Long userId = 1L;
    UpdatePhoneRequest request = new UpdatePhoneRequest();
    request.setIdToken("valid-id-token");

    FirebaseToken firebaseToken = org.mockito.Mockito.mock(FirebaseToken.class);
    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", null);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      when(firebaseAuthService.verifyIdToken("valid-id-token")).thenReturn(firebaseToken);
      when(firebaseToken.getUid()).thenReturn("uid");
      when(firebaseToken.getClaims()).thenReturn(claims);

      assertThrows(AppException.class, () -> userService.updatePhone(request));
    }
  }

  @Test
  void updatePhone_WithPhoneUsedByOtherUser_ThrowsException() {
    Long userId = 1L;
    String newPhone = "+84123456789";
    UpdatePhoneRequest request = new UpdatePhoneRequest();
    request.setIdToken("valid-id-token");

    FirebaseToken firebaseToken = org.mockito.Mockito.mock(FirebaseToken.class);
    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", newPhone);

    User otherUser = User.builder().id(99L).phoneNumber(newPhone).build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      when(firebaseAuthService.verifyIdToken("valid-id-token")).thenReturn(firebaseToken);
      when(firebaseToken.getUid()).thenReturn("uid");
      when(firebaseToken.getClaims()).thenReturn(claims);
      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userRepository.findByPhoneNumber(newPhone)).thenReturn(Optional.of(otherUser));

      assertThrows(AppException.class, () -> userService.updatePhone(request));
      verify(userRepository, never()).save(any());
    }
  }

  @Test
  void updatePhone_WithSameUserPhone_Succeeds() {
    Long userId = 1L;
    String newPhone = "+84123456789";
    UpdatePhoneRequest request = new UpdatePhoneRequest();
    request.setIdToken("valid-id-token");

    FirebaseToken firebaseToken = org.mockito.Mockito.mock(FirebaseToken.class);
    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", newPhone);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      when(firebaseAuthService.verifyIdToken("valid-id-token")).thenReturn(firebaseToken);
      when(firebaseToken.getUid()).thenReturn("new-uid");
      when(firebaseToken.getClaims()).thenReturn(claims);
      when(userHelper.findUserById(userId)).thenReturn(testUser);
      when(userRepository.findByPhoneNumber(newPhone)).thenReturn(Optional.of(testUser));
      when(userRepository.save(testUser)).thenReturn(testUser);

      ApiResponseDTO<UpdatePhoneResponse> response = userService.updatePhone(request);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    }
  }
}
