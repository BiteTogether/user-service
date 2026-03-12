package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;
import com.bitetogether.user.enums.FriendRequestType;
import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.service.EventPublisherService;
import com.bitetogether.user.util.AuthUtils;
import com.bitetogether.user.util.UserHelper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"java:S2699", "java:S6073"})
class FriendRequestServiceImplTest {

  @Mock private FriendRequestRepository friendRequestRepository;

  @Mock private UserMapper userMapper;

  @Mock private UserHelper userHelper;

  @Mock private EventPublisherService eventPublisherService;

  @InjectMocks private FriendRequestServiceImpl friendRequestService;

  private User sender;
  private User receiver;
  private FriendRequest friendRequest;

  @BeforeEach
  void setUp() {
    sender = User.builder().id(1L).username("sender").friends(new HashSet<>()).build();

    receiver = User.builder().id(2L).username("receiver").friends(new HashSet<>()).build();

    friendRequest = FriendRequest.builder().id(1L).sender(sender).receiver(receiver).build();
  }

  @Test
  void createFriendRequest_WithValidUsers_ReturnsFriendRequestId() {
    Long senderId = 1L;
    Long receiverId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(userHelper.findUserById(senderId)).thenReturn(sender);
      when(userHelper.findUserById(receiverId)).thenReturn(receiver);
      when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(false);
      when(friendRequestRepository.existsBySenderAndReceiver(receiver, sender)).thenReturn(false);
      when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(friendRequest);

      ApiResponseDTO<Long> response = friendRequestService.createFriendRequest(receiverId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend request sent successfully", response.getMessage());
      assertEquals(1L, response.getData());

      verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
    }
  }

  @Test
  void createFriendRequest_WhenSenderEqualsReceiver_ThrowsException() {
    Long userId = 1L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      AppException exception =
          assertThrows(AppException.class, () -> friendRequestService.createFriendRequest(userId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).save(any());
    }
  }

  @Test
  void createFriendRequest_WhenAlreadyFriends_ThrowsException() {
    Long senderId = 1L;
    Long receiverId = 2L;
    sender.getFriends().add(receiver);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(userHelper.findUserById(senderId)).thenReturn(sender);
      when(userHelper.findUserById(receiverId)).thenReturn(receiver);

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.createFriendRequest(receiverId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).save(any());
    }
  }

  @Test
  void createFriendRequest_WhenReceiverHasSenderAsFriend_ThrowsException() {
    Long senderId = 1L;
    Long receiverId = 2L;
    receiver.getFriends().add(sender);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(userHelper.findUserById(senderId)).thenReturn(sender);
      when(userHelper.findUserById(receiverId)).thenReturn(receiver);

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.createFriendRequest(receiverId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).save(any());
    }
  }

  @Test
  void createFriendRequest_WhenRequestAlreadyExists_ThrowsException() {
    Long senderId = 1L;
    Long receiverId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(userHelper.findUserById(senderId)).thenReturn(sender);
      when(userHelper.findUserById(receiverId)).thenReturn(receiver);
      when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(true);

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.createFriendRequest(receiverId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).save(any());
    }
  }

  @Test
  void createFriendRequest_WhenReverseRequestAlreadyExists_ThrowsException() {
    Long senderId = 1L;
    Long receiverId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(userHelper.findUserById(senderId)).thenReturn(sender);
      when(userHelper.findUserById(receiverId)).thenReturn(receiver);
      when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(false);
      when(friendRequestRepository.existsBySenderAndReceiver(receiver, sender)).thenReturn(true);

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.createFriendRequest(receiverId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).save(any());
    }
  }

  @Test
  void acceptFriendRequest_WithValidRequest_EstablishesFriendship() {
    Long requestId = 1L;
    Long receiverId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(receiverId);

      when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(friendRequest));
      when(userHelper.saveUser(sender)).thenReturn(sender);
      when(userHelper.saveUser(receiver)).thenReturn(receiver);

      ApiResponseDTO<Void> response = friendRequestService.acceptFriendRequest(requestId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend request accepted successfully", response.getMessage());

      verify(userHelper, times(1)).saveUser(sender);
      verify(userHelper, times(1)).saveUser(receiver);
      verify(friendRequestRepository, times(1)).deleteById(requestId);
      verify(eventPublisherService, times(1)).publishCreateConversationEvent(any());
    }
  }

  @Test
  void acceptFriendRequest_WhenRequestNotFound_ThrowsException() {
    Long requestId = 1L;

    when(friendRequestRepository.findById(requestId)).thenReturn(Optional.empty());

    AppException exception =
        assertThrows(AppException.class, () -> friendRequestService.acceptFriendRequest(requestId));

    assertNotNull(exception);
    verify(friendRequestRepository, never()).deleteById(any());
  }

  @Test
  void acceptFriendRequest_WhenNotReceiver_ThrowsException() {
    Long requestId = 1L;
    Long wrongUserId = 3L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(wrongUserId);

      when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(friendRequest));

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.acceptFriendRequest(requestId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).deleteById(any());
    }
  }

  @Test
  void deleteFriendRequest_WhenSenderDeletes_ReturnsWithdrawnMessage() {
    Long requestId = 1L;
    Long senderId = 1L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(friendRequest));

      ApiResponseDTO<String> response = friendRequestService.deleteFriendRequest(requestId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend request withdrawn successfully", response.getMessage());

      verify(friendRequestRepository, times(1)).deleteById(requestId);
    }
  }

  @Test
  void deleteFriendRequest_WhenReceiverDeletes_ReturnsRejectedMessage() {
    Long requestId = 1L;
    Long receiverId = 2L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(receiverId);

      when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(friendRequest));

      ApiResponseDTO<String> response = friendRequestService.deleteFriendRequest(requestId);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Friend request rejected successfully", response.getMessage());

      verify(friendRequestRepository, times(1)).deleteById(requestId);
    }
  }

  @Test
  void deleteFriendRequest_WhenRequestNotFound_ThrowsException() {
    Long requestId = 1L;
    Long userId = 1L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      when(friendRequestRepository.findById(requestId)).thenReturn(Optional.empty());

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.deleteFriendRequest(requestId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).deleteById(any());
    }
  }

  @Test
  void deleteFriendRequest_WhenNotInvolvedInRequest_ThrowsException() {
    Long requestId = 1L;
    Long wrongUserId = 3L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(wrongUserId);

      when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(friendRequest));

      AppException exception =
          assertThrows(
              AppException.class, () -> friendRequestService.deleteFriendRequest(requestId));

      assertNotNull(exception);
      verify(friendRequestRepository, never()).deleteById(any());
    }
  }

  @Test
  void getSentFriendRequests_WithValidPagination_ReturnsRequests() {
    Long senderId = 1L;
    int page = 0;
    int size = 10;
    Pageable pageable = PageRequest.of(page, size);
    List<FriendRequest> requests = List.of(friendRequest);
    Page<FriendRequest> requestPage = new PageImpl<>(requests, pageable, 1);
    FriendResponse friendResponse = FriendResponse.builder().id(2L).username("receiver").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(senderId);

      when(friendRequestRepository.findBySenderId(senderId, pageable)).thenReturn(requestPage);
      when(userMapper.toFriendResponse(receiver)).thenReturn(friendResponse);

      ApiResponsePaginationDTO<FriendRequestResponse> response =
          friendRequestService.getSentFriendRequests(page, size);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Sent friend requests retrieved successfully", response.getMessage());
      assertEquals(1, response.getData().size());
      assertEquals(0, response.getCurrentPage());
      assertEquals(1, response.getTotalPages());

      verify(friendRequestRepository, times(1)).findBySenderId(senderId, pageable);
    }
  }

  @Test
  void getReceivedFriendRequests_WithValidPagination_ReturnsRequests() {
    Long receiverId = 2L;
    int page = 0;
    int size = 10;
    Pageable pageable = PageRequest.of(page, size);
    List<FriendRequest> requests = List.of(friendRequest);
    Page<FriendRequest> requestPage = new PageImpl<>(requests, pageable, 1);
    FriendResponse friendResponse = FriendResponse.builder().id(1L).username("sender").build();

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(receiverId);

      when(friendRequestRepository.findByReceiverId(receiverId, pageable)).thenReturn(requestPage);
      when(userMapper.toFriendResponse(sender)).thenReturn(friendResponse);

      ApiResponsePaginationDTO<FriendRequestResponse> response =
          friendRequestService.getReceivedFriendRequests(page, size);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Received friend requests retrieved successfully", response.getMessage());
      assertEquals(1, response.getData().size());
      assertEquals(0, response.getCurrentPage());
      assertEquals(1, response.getTotalPages());

      verify(friendRequestRepository, times(1)).findByReceiverId(receiverId, pageable);
    }
  }

  @Test
  void getFriendRequestTypeBetweenUsers_WhenSentRequestExists_ReturnsSent() {
    when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(true);

    FriendRequestType result =
        friendRequestService.getFriendRequestTypeBetweenUsers(sender, receiver);

    assertEquals(FriendRequestType.SENT, result);
    verify(friendRequestRepository, times(1)).existsBySenderAndReceiver(sender, receiver);
  }

  @Test
  void getFriendRequestTypeBetweenUsers_WhenReceivedRequestExists_ReturnsReceived() {
    when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(false);
    when(friendRequestRepository.existsBySenderAndReceiver(receiver, sender)).thenReturn(true);

    FriendRequestType result =
        friendRequestService.getFriendRequestTypeBetweenUsers(sender, receiver);

    assertEquals(FriendRequestType.RECEIVED, result);
    verify(friendRequestRepository, times(1)).existsBySenderAndReceiver(sender, receiver);
    verify(friendRequestRepository, times(1)).existsBySenderAndReceiver(receiver, sender);
  }

  @Test
  void getFriendRequestTypeBetweenUsers_WhenNoRequestExists_ReturnsNone() {
    when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(false);
    when(friendRequestRepository.existsBySenderAndReceiver(receiver, sender)).thenReturn(false);

    FriendRequestType result =
        friendRequestService.getFriendRequestTypeBetweenUsers(sender, receiver);

    assertEquals(FriendRequestType.NONE, result);
    verify(friendRequestRepository, times(1)).existsBySenderAndReceiver(sender, receiver);
    verify(friendRequestRepository, times(1)).existsBySenderAndReceiver(receiver, sender);
  }

  @Test
  void getFriendRequestSentId_WhenRequestExists_ReturnsId() {
    when(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
        .thenReturn(Optional.of(friendRequest));

    Long result = friendRequestService.getFriendRequestSentId(sender, receiver);

    assertEquals(1L, result);
    verify(friendRequestRepository, times(1)).findBySenderAndReceiver(sender, receiver);
  }

  @Test
  void getFriendRequestSentId_WhenRequestNotExists_ReturnsNull() {
    when(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
        .thenReturn(Optional.empty());

    Long result = friendRequestService.getFriendRequestSentId(sender, receiver);

    assertNull(result);
    verify(friendRequestRepository, times(1)).findBySenderAndReceiver(sender, receiver);
  }

  @Test
  void getFriendRequestReceivedId_WhenRequestExists_ReturnsId() {
    when(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
        .thenReturn(Optional.of(friendRequest));

    Long result = friendRequestService.getFriendRequestReceivedId(receiver, sender);

    assertEquals(1L, result);
    verify(friendRequestRepository, times(1)).findBySenderAndReceiver(sender, receiver);
  }

  @Test
  void getFriendRequestReceivedId_WhenRequestNotExists_ReturnsNull() {
    when(friendRequestRepository.findBySenderAndReceiver(sender, receiver))
        .thenReturn(Optional.empty());

    Long result = friendRequestService.getFriendRequestReceivedId(receiver, sender);

    assertNull(result);
    verify(friendRequestRepository, times(1)).findBySenderAndReceiver(sender, receiver);
  }
}
