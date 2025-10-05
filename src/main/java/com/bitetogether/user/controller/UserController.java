package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.HAS_ROLE_ADMIN;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_USER;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_USER)
@Slf4j
public class UserController {
  private final UserService userService;

  @PreAuthorize(HAS_ROLE_ADMIN)
  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createUser(
      @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(userService.createUser(createUserRequest));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable Long id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
    return buildEntityResponse(userService.updateUser(id, updateUserRequest));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
    return buildEntityResponse(userService.deleteUser(id));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserDetailsResponse>> getCurrentUser() {
    return buildEntityResponse(userService.getCurrentUser());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserDetailsResponse>> getUserById(@PathVariable Long id) {
    return buildEntityResponse(userService.getUserById(id));
  }
}
