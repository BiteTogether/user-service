package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_FRIEND;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.service.FriendService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_FRIEND)
@Slf4j
public class FriendController {
  private final FriendService friendService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriendsList() {
    return buildEntityResponse(friendService.getFriendsList());
  }

  @PostMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteFriend(@PathVariable Long id) {
    return buildEntityResponse(friendService.deleteFriend(id));
  }
}
