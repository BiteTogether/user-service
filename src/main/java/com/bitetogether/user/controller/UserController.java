package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createUser(
      @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(userService.createUser(createUserRequest));
  }
}
