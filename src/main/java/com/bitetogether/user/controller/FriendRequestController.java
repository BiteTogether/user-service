package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.DEFAULT_PAGE_NUMBER;
import static com.bitetogether.common.util.Constants.DEFAULT_PAGE_SIZE;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_FRIEND_REQUEST;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.validation.ValidLongId;
import com.bitetogether.common.validation.ValidPage;
import com.bitetogether.common.validation.ValidSize;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;
import com.bitetogether.user.service.FriendRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_FRIEND_REQUEST)
@Validated
@Tag(
    name = "Friend Requests",
    description =
        "APIs for managing friend requests including sending, accepting, rejecting, and viewing pending requests")
public class FriendRequestController {
  private final FriendRequestService friendRequestService;

  @Operation(
      summary = "Send friend request",
      description =
          "Sends a friend request from the authenticated user to another user specified by the receiver ID")
  @PostMapping("/{receiverId}")
  public ResponseEntity<ApiResponseDTO<Long>> createFriendRequest(@PathVariable Long receiverId) {
    return buildEntityResponse(friendRequestService.createFriendRequest(receiverId));
  }

  @Operation(
      summary = "Accept friend request",
      description =
          "Accepts a pending friend request. This creates a friendship connection between the two users")
  @PostMapping("/{id}/accept")
  public ResponseEntity<ApiResponseDTO<Void>> acceptFriendRequest(
      @PathVariable @ValidLongId Long id) {
    return buildEntityResponse(friendRequestService.acceptFriendRequest(id));
  }

  @Operation(
      summary = "Delete/reject friend request",
      description =
          "Deletes or rejects a friend request. Can be used to cancel a sent request or reject a received request")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<String>> deleteFriendRequest(
      @PathVariable @ValidLongId Long id) {
    return buildEntityResponse(friendRequestService.deleteFriendRequest(id));
  }

  @Operation(
      summary = "Get sent friend requests",
      description =
          "Retrieves a paginated list of all friend requests sent by the authenticated user that are still pending")
  @GetMapping("/sent")
  public ResponseEntity<ApiResponsePaginationDTO<FriendRequestResponse>> getSentFriendRequests(
      @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) @ValidPage int page,
      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @ValidSize int size) {
    return buildEntityResponse(friendRequestService.getSentFriendRequests(page, size));
  }

  @Operation(
      summary = "Get received friend requests",
      description =
          "Retrieves a paginated list of all pending friend requests received by the authenticated user")
  @GetMapping("/received")
  public ResponseEntity<ApiResponsePaginationDTO<FriendRequestResponse>> getReceivedFriendRequests(
      @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) @ValidPage int page,
      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @ValidSize int size) {
    return buildEntityResponse(friendRequestService.getReceivedFriendRequests(page, size));
  }
}
