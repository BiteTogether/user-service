package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_FRIEND_REQUEST;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;
import com.bitetogether.user.service.FriendRequestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_FRIEND_REQUEST)
public class FriendRequestController {
  private final FriendRequestService friendRequestService;

  @PostMapping("/{receiverId}")
  public ResponseEntity<ApiResponse<Long>> createFriendRequest(@PathVariable Long receiverId) {
    return buildEntityResponse(friendRequestService.createFriendRequest(receiverId));
  }

  @PostMapping("/{id}/accept")
  public ResponseEntity<ApiResponse<Void>> acceptFriendRequest(@PathVariable Long id) {
    return buildEntityResponse(friendRequestService.acceptFriendRequest(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteFriendRequest(@PathVariable Long id) {
    return buildEntityResponse(friendRequestService.deleteFriendRequest(id));
  }

  @GetMapping("/sent")
  public ResponseEntity<ApiResponsePagination<List<FriendRequestResponse>>> getSentFriendRequests(
      @RequestBody PaginationRequest paginationRequest) {
    return buildEntityResponse(friendRequestService.getSentFriendRequests(paginationRequest));
  }

  @GetMapping("/received")
  public ResponseEntity<ApiResponsePagination<List<FriendRequestResponse>>>
      getReceivedFriendRequests(@RequestBody PaginationRequest paginationRequest) {
    return buildEntityResponse(friendRequestService.getReceivedFriendRequests(paginationRequest));
  }
}
