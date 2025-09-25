package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.GlobalErrorCode;
import com.bitetogether.user.convert.FriendRequestMapper;
import com.bitetogether.user.dto.friendrequest.request.CreateFriendRequestRequest;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.service.FriendRequestService;
import com.bitetogether.user.util.UserHelper;
import jakarta.transaction.Transactional;
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
  FriendRequestMapper friendRequestMapper;
  UserHelper userHelper;

  @Override
  @Transactional
  public ApiResponse<Long> createFriendRequest(
      CreateFriendRequestRequest createFriendRequestRequest) {
    Long receiverId = createFriendRequestRequest.getReceiverId();
    Long senderId = getCurrentUserId();

    validateCreateFriendRequest(senderId, receiverId);

    User sender = userHelper.findUserById(senderId);
    User receiver = userHelper.findUserById(receiverId);

    FriendRequest friendRequest = FriendRequest.builder().sender(sender).receiver(receiver).build();

    FriendRequest friendRequestDb = friendRequestRepository.save(friendRequest);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "Friend request sent successfully", friendRequestDb.getId());
  }

  @Override
  @Transactional
  public ApiResponse<Void> acceptFriendRequest(Long id) {
    FriendRequest friendRequest = validateAcceptFriendRequest(id);

    User sender = friendRequest.getSender();
    User receiver = friendRequest.getReceiver();

    sender.getFriendList().add(receiver);
    receiver.getFriendList().add(sender);

    deleteFriendRequestHelper(friendRequest);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "Friend request accepted successfully", null);
  }

  @Override
  @Transactional
  public ApiResponse<String> deleteFriendRequest(Long id) {
    Long currentUserId = getCurrentUserId();

    FriendRequest friendRequest = validateDeleteFriendRequest(id, currentUserId);

    deleteFriendRequestHelper(friendRequest);

    String message =
        friendRequest.getSender().getId().equals(currentUserId)
            ? "Friend request withdrawn successfully"
            : "Friend request rejected successfully";

    return buildApiResponse(ApiResponseStatus.SUCCESS, message, null);
  }

  @Override
  public ApiResponsePagination<List<FriendRequestResponse>> getSentFriendRequests(
      PaginationRequest paginationRequest) {
    Long currentUserId = getCurrentUserId();
    User currentUser = userHelper.findUserById(currentUserId);

    Pageable pageable = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize());

    Page<FriendRequest> friendRequestPage =
        friendRequestRepository.findBySenderId(currentUser.getId(), pageable);
    List<FriendRequestResponse> friendRequestResponses =
        friendRequestPage.getContent().stream()
            .map(
                friendRequest -> {
                  User receiver = friendRequest.getReceiver();
                  return friendRequestMapper.toFriendRequestResponse(receiver);
                })
            .toList();

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Sent friend requests retrieved successfully",
        friendRequestResponses,
        friendRequestPage.getNumber(),
        friendRequestPage.getTotalElements(),
        friendRequestPage.getTotalPages());
  }

  @Override
  public ApiResponsePagination<List<FriendRequestResponse>> getReceivedFriendRequests(
      PaginationRequest paginationRequest) {
    Long currentUserId = getCurrentUserId();
    User currentUser = userHelper.findUserById(currentUserId);

    Pageable pageable = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize());

    Page<FriendRequest> friendRequestPage =
        friendRequestRepository.findByReceiverId(currentUser.getId(), pageable);
    List<FriendRequestResponse> friendRequestResponses =
        friendRequestPage.stream()
            .map(
                friendRequest -> {
                  User sender = friendRequest.getSender();
                  return friendRequestMapper.toFriendRequestResponse(sender);
                })
            .toList();

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Received friend requests retrieved successfully",
        friendRequestResponses,
        friendRequestPage.getNumber(),
        friendRequestPage.getTotalElements(),
        friendRequestPage.getTotalPages());
  }

  private void deleteFriendRequestHelper(FriendRequest friendRequest) {
    friendRequestRepository.delete(friendRequest);
  }

  private void validateCreateFriendRequest(Long senderId, Long receiverId) {
    if (senderId.equals(receiverId)) {
      throw new AppException(ErrorCode.INVALID_FRIEND_REQUEST);
    }

    User sender = userHelper.findUserById(senderId);
    User receiver = userHelper.findUserById(receiverId);

    if (sender.getFriendList().stream().anyMatch(friend -> friend.getId().equals(receiverId))) {
      throw new AppException(ErrorCode.ALREADY_FRIENDS);
    }

    boolean existingRequest =
        friendRequestRepository.existsBySenderAndReceiver(sender, receiver)
            || friendRequestRepository.existsBySenderAndReceiver(receiver, sender);

    if (existingRequest) {
      throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
    }
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
}
