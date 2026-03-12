package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.DEFAULT_PAGE_NUMBER;
import static com.bitetogether.common.util.Constants.DEFAULT_PAGE_SIZE;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_FRIEND;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.validation.ValidPage;
import com.bitetogether.common.validation.ValidSize;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_FRIEND)
@Slf4j
@Validated
@Tag(name = "Friends", description = "APIs for managing established friendships between users")
public class FriendController {
  private final FriendService friendService;

  @Operation(
      summary = "Get friends list",
      description =
          "Retrieves a paginated list of all friends for the currently authenticated user")
  @GetMapping
  public ResponseEntity<ApiResponsePaginationDTO<FriendResponse>> getFriendsList(
      @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) @ValidPage int page,
      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @ValidSize int size) {
    return buildEntityResponse(friendService.getFriendsList(page, size));
  }

  @Operation(
      summary = "Remove friend",
      description =
          "Removes a friend connection between the authenticated user and another user. This action unfriends the user")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<String>> deleteFriend(@PathVariable Long id) {
    return buildEntityResponse(friendService.deleteFriend(id));
  }
}
