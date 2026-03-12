package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.util.AuthUtils;
import com.bitetogether.user.util.UserHelper;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
  "java:S2699",
  "java:S6073"
}) // Sonar: assertions present, unboxing warnings are false positives
class FriendServiceImplTest {

  @Mock private UserMapper userMapper;

  @Mock private UserHelper userHelper;

  @Mock private UserRepository userRepository;

  @InjectMocks private FriendServiceImpl friendService;

  private User currentUser;
  private User friendUser;

  @BeforeEach
  void setUp() {
    currentUser = User.builder().id(1L).username("currentuser").friends(new HashSet<>()).build();

    friendUser = User.builder().id(2L).username("frienduser").friends(new HashSet<>()).build();
  }

  @Test
  void getFriendsList_WithValidPagination_ReturnsFriendsList() {
    Long currentUserId = 1L;
    int page = 0;
    int size = 10;
    Pageable pageable = PageRequest.of(page, size);
    List<User> friends = List.of(friendUser);
    Page<User> friendPage = new PageImpl<>(friends, pageable, 1);
    FriendResponse friendResponse = FriendResponse.builder().id(2L).username("frienduser").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.getFriendsByUserId(currentUserId, pageable)).thenReturn(friendPage);
      when(userMapper.toFriendResponse(friendUser)).thenReturn(friendResponse);

      ApiResponsePaginationDTO<FriendResponse> response = friendService.getFriendsList(page, size);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend list retrieved successfully", response.getMessage());
      assertEquals(1, response.getData().size());
      assertEquals(0, response.getCurrentPage());
      assertEquals(1, response.getTotalPages());

      verify(userRepository, times(1)).getFriendsByUserId(currentUserId, pageable);
      verify(userMapper, times(1)).toFriendResponse(friendUser);
    }
  }

  @Test
  void getFriendsList_WithEmptyFriendsList_ReturnsEmptyList() {
    Long currentUserId = 1L;
    int page = 0;
    int size = 10;
    Pageable pageable = PageRequest.of(page, size);
    List<User> friends = List.of();
    Page<User> friendPage = new PageImpl<>(friends, pageable, 0);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);

      when(userRepository.getFriendsByUserId(currentUserId, pageable)).thenReturn(friendPage);

      ApiResponsePaginationDTO<FriendResponse> response = friendService.getFriendsList(page, size);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals(0, response.getData().size());

      verify(userRepository, times(1)).getFriendsByUserId(currentUserId, pageable);
      verify(userMapper, never()).toFriendResponse(friendUser);
    }
  }

  @Test
  void deleteFriend_WithValidFriend_DeletesFriendship() {
    Long currentUserId = 1L;
    Long friendId = 2L;
    currentUser.getFriends().add(friendUser);
    friendUser.getFriends().add(currentUser);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(currentUserId)).thenReturn(currentUser);
      when(userHelper.findFriendById(friendId)).thenReturn(friendUser);
      when(userHelper.saveUser(currentUser)).thenReturn(currentUser);
      when(userHelper.saveUser(friendUser)).thenReturn(friendUser);

      ApiResponseDTO<String> response = friendService.deleteFriend(friendId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend removed successfully", response.getMessage());

      verify(userHelper, times(1)).saveUser(currentUser);
      verify(userHelper, times(1)).saveUser(friendUser);
    }
  }

  @Test
  void deleteFriend_WhenNotFriends_ThrowsException() {
    Long currentUserId = 1L;
    Long friendId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(currentUserId)).thenReturn(currentUser);
      when(userHelper.findFriendById(friendId)).thenReturn(friendUser);

      AppException exception =
          assertThrows(AppException.class, () -> friendService.deleteFriend(friendId));

      assertNotNull(exception);
      verify(userHelper, never()).saveUser(currentUser);
      verify(userHelper, never()).saveUser(friendUser);
    }
  }

  @Test
  void deleteFriend_AsAdmin_SkipsValidation() {
    Long currentUserId = 1L;
    Long friendId = 2L;
    friendUser.getFriends().add(currentUser);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(false);

      when(userHelper.findUserById(currentUserId)).thenReturn(currentUser);
      when(userHelper.findFriendById(friendId)).thenReturn(friendUser);
      when(userHelper.saveUser(currentUser)).thenReturn(currentUser);
      when(userHelper.saveUser(friendUser)).thenReturn(friendUser);

      ApiResponseDTO<String> response = friendService.deleteFriend(friendId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend removed successfully", response.getMessage());

      verify(userHelper, times(1)).saveUser(currentUser);
      verify(userHelper, times(1)).saveUser(friendUser);
    }
  }

  @Test
  void deleteFriend_WithMultipleFriends_RemovesOnlyTargetFriend() {
    Long currentUserId = 1L;
    Long friendId = 2L;
    User anotherFriend =
        User.builder().id(3L).username("anotherfriend").friends(new HashSet<>()).build();

    currentUser.getFriends().add(friendUser);
    currentUser.getFriends().add(anotherFriend);
    friendUser.getFriends().add(currentUser);
    anotherFriend.getFriends().add(currentUser);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(() -> AuthUtils.hasRole(Role.USER.name())).thenReturn(true);

      when(userHelper.findUserById(currentUserId)).thenReturn(currentUser);
      when(userHelper.findFriendById(friendId)).thenReturn(friendUser);
      when(userHelper.saveUser(currentUser)).thenReturn(currentUser);
      when(userHelper.saveUser(friendUser)).thenReturn(friendUser);

      ApiResponseDTO<String> response = friendService.deleteFriend(friendId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals(1, currentUser.getFriends().size());
      assertEquals(0, friendUser.getFriends().size());

      verify(userHelper, times(1)).saveUser(currentUser);
      verify(userHelper, times(1)).saveUser(friendUser);
    }
  }
}
