package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.user.util.AuthUtils.getCurrentUserId;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.GlobalErrorCode;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.event.CreateConversationEvent;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;
import com.bitetogether.user.enums.FriendRequestType;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.service.EventPublisherService;
import com.bitetogether.user.service.FriendRequestService;
import com.bitetogether.user.util.UserHelper;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendRequestServiceImpl implements FriendRequestService {
  FriendRequestRepository friendRequestRepository;
  UserMapper userMapper;
  UserHelper userHelper;
  EventPublisherService eventPublisherService;

  @Override
  @Transactional
  public ApiResponseDTO<Long> createFriendRequest(Long receiverId) {
    Long senderId = getCurrentUserId();

    validateCreateFriendRequest(senderId, receiverId);

    User sender = userHelper.findUserById(senderId);
    User receiver = userHelper.findUserById(receiverId);

    FriendRequest friendRequest = FriendRequest.builder().sender(sender).receiver(receiver).build();

    FriendRequest friendRequestDb = friendRequestRepository.save(friendRequest);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "Friend request sent successfully", friendRequestDb.getId());
  }

  private void validateCreateFriendRequest(Long senderId, Long receiverId) {
    if (senderId.equals(receiverId)) {
      throw new AppException(ErrorCode.INVALID_FRIEND_REQUEST);
    }

    User sender = userHelper.findUserById(senderId);
    User receiver = userHelper.findUserById(receiverId);

    boolean isFriended =
        sender.getFriends().contains(receiver) || receiver.getFriends().contains(sender);
    if (isFriended) {
      throw new AppException(ErrorCode.ALREADY_FRIENDS);
    }

    boolean existingRequest =
        friendRequestRepository.existsBySenderAndReceiver(sender, receiver)
            || friendRequestRepository.existsBySenderAndReceiver(receiver, sender);

    if (existingRequest) {
      throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
    }
  }

  @Override
  @Transactional
  public ApiResponseDTO<FriendResponse> acceptFriendRequest(Long id) {
    FriendRequest friendRequest = validateAcceptFriendRequest(id);

    User sender = friendRequest.getSender();

    establishFriendship(friendRequest);

    // Publish event to create a direct conversation
    publishCreateConversationEvent(
        friendRequest.getSender().getId(), friendRequest.getReceiver().getId());

    deleteFriendRequestHelper(friendRequest.getId());

    FriendResponse senderResponse = userMapper.toFriendResponse(sender);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "Friend request accepted successfully", senderResponse);
  }

  private FriendRequest validateAcceptFriendRequest(Long requestId) {
    FriendRequest friendRequest =
        friendRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

    if (!friendRequest.getReceiver().getId().equals(getCurrentUserId())) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }

    return friendRequest;
  }

  private void establishFriendship(FriendRequest friendRequest) {
    User sender = friendRequest.getSender();
    User receiver = friendRequest.getReceiver();

    sender.getFriends().add(receiver);
    receiver.getFriends().add(sender);

    userHelper.saveUser(sender);
    userHelper.saveUser(receiver);
  }

  private void deleteFriendRequestHelper(Long id) {
    friendRequestRepository.deleteById(id);
  }

  @Override
  @Transactional
  public ApiResponseDTO<String> deleteFriendRequest(Long id) {
    Long currentUserId = getCurrentUserId();

    FriendRequest friendRequest = validateDeleteFriendRequest(id, currentUserId);

    deleteFriendRequestHelper(friendRequest.getId());

    String message =
        friendRequest.getSender().getId().equals(currentUserId)
            ? "Friend request withdrawn successfully"
            : "Friend request rejected successfully";

    return buildApiResponse(ApiResponseStatus.SUCCESS, message, null);
  }

  private FriendRequest validateDeleteFriendRequest(Long requestId, Long currentUserId) {
    FriendRequest friendRequest =
        friendRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

    if (!friendRequest.getSender().getId().equals(currentUserId)
        && !friendRequest.getReceiver().getId().equals(currentUserId)) {
      throw new AppException(GlobalErrorCode.USER_FORBIDDEN);
    }

    return friendRequest;
  }

  @Override
  public ApiResponsePaginationDTO<FriendRequestResponse> getSentFriendRequests(int page, int size) {
    Long currentUserId = getCurrentUserId();

    Pageable pageable = PageRequest.of(page, size);
    Page<FriendRequest> friendRequestPage =
        friendRequestRepository.findBySenderId(currentUserId, pageable);

    List<FriendRequestResponse> friendRequestResponses =
        friendRequestPage.getContent().stream()
            .map(
                friendRequest -> {
                  User receiver = friendRequest.getReceiver();
                  return FriendRequestResponse.builder()
                      .id(friendRequest.getId())
                      .user(userMapper.toFriendResponse(receiver))
                      .build();
                })
            .toList();

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Sent friend requests retrieved successfully",
        friendRequestResponses,
        friendRequestPage.getNumber(),
        friendRequestPage.getTotalPages(),
        friendRequestPage.getTotalElements());
  }

  @Override
  public ApiResponsePaginationDTO<FriendRequestResponse> getReceivedFriendRequests(
      int page, int size) {
    Long currentUserId = getCurrentUserId();

    Pageable pageable = PageRequest.of(page, size);
    Page<FriendRequest> friendRequestPage =
        friendRequestRepository.findByReceiverId(currentUserId, pageable);

    List<FriendRequestResponse> friendRequestResponses =
        friendRequestPage.getContent().stream()
            .map(
                friendRequest -> {
                  User sender = friendRequest.getSender();
                  return FriendRequestResponse.builder()
                      .id(friendRequest.getId())
                      .user(userMapper.toFriendResponse(sender))
                      .build();
                })
            .toList();

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Received friend requests retrieved successfully",
        friendRequestResponses,
        friendRequestPage.getNumber(),
        friendRequestPage.getTotalPages(),
        friendRequestPage.getTotalElements());
  }

  public FriendRequestType getFriendRequestTypeBetweenUsers(User sender, User receiver) {
    boolean sentRequestExists = friendRequestRepository.existsBySenderAndReceiver(sender, receiver);
    if (sentRequestExists) {
      return FriendRequestType.SENT;
    }

    boolean receivedRequestExists =
        friendRequestRepository.existsBySenderAndReceiver(receiver, sender);
    if (receivedRequestExists) {
      return FriendRequestType.RECEIVED;
    }

    return FriendRequestType.NONE;
  }

  public Long getFriendRequestSentId(User sender, User receiver) {
    return friendRequestRepository
        .findBySenderAndReceiver(sender, receiver)
        .map(FriendRequest::getId)
        .orElse(null);
  }

  public Long getFriendRequestReceivedId(User receiver, User sender) {
    return friendRequestRepository
        .findBySenderAndReceiver(sender, receiver)
        .map(FriendRequest::getId)
        .orElse(null);
  }

  // ==================== KAFKA EVENT PUBLISHERS ====================

  private void publishCreateConversationEvent(Long user1Id, Long user2Id) {
    CreateConversationEvent event =
        CreateConversationEvent.builder()
            .user1Id(user1Id)
            .user2Id(user2Id)
            .eventTimestamp(LocalDateTime.now())
            .build();

    eventPublisherService.publishCreateConversationEvent(event);
  }
}
