package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.response.ListUserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.enums.FriendRequestType;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.ConversationService;
import com.bitetogether.user.service.EventPublisherService;
import com.bitetogether.user.util.AuthUtils;
import com.bitetogether.user.util.UserHelper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
