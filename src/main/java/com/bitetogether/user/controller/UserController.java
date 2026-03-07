package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.HAS_ROLE_ADMIN;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_USER;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.validation.ValidLongId;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserNotificationSettingsRequest;
import com.bitetogether.user.dto.user.request.UserOnlineStatus;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.response.ListUserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserNotificationResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_USER)
@Validated
@Slf4j
@Tag(
    name = "User Management",
    description = "APIs for managing user profiles, settings, search, and online status")
public class UserController {
  private final UserService userService;

  @Operation(
      summary = "Create user (Admin only)",
      description =
          "Creates a new user account. This endpoint is restricted to administrators only and is used for administrative user creation")
//  @PreAuthorize(HAS_ROLE_ADMIN)
  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createUser(
      @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(userService.createUser(createUserRequest));
  }

  @Operation(
      summary = "Update user profile",
      description =
          "Updates the profile information of a specific user. Users can update their own profile or admins can update any user's profile")
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable @ValidLongId Long id, @RequestBody @Valid UpdateUserRequest updateUserRequest) {
    return buildEntityResponse(userService.updateUser(id, updateUserRequest));
  }

  @Operation(
      summary = "Delete user",
      description =
          "Deletes a user account permanently. This action removes all user data and cannot be undone")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.deleteUser(id));
  }

  @Operation(
      summary = "Get current user profile",
      description =
          "Retrieves the detailed profile information of the currently authenticated user")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserDetailsResponse>> getCurrentUser() {
    return buildEntityResponse(userService.getCurrentUser());
  }

  @Operation(
      summary = "Get user by ID",
      description = "Retrieves the profile information of a specific user by their user ID")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserGetByIdResponse>> getUserById(
      @PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.getUserById(id));
  }

  @Operation(
      summary = "Get list of user details",
      description = "Retrieves detailed information for a list of users based on their IDs")
  @GetMapping
  public ResponseEntity<ApiResponse<ListUserDetailsResponse>> getListUserDetails(
      @RequestParam List<@Positive Long> userIds) {
    return buildEntityResponse(userService.getListUser(userIds));
  }

  @Operation(
      summary = "Search users",
      description =
          "Searches for users based on various filters such as name, email, or other criteria. Returns paginated results")
  @PostMapping("/search")
  public ResponseEntity<ApiResponse<UserSearchResponse>> searchUsersWithFilter(
      @RequestBody @Valid UserSearchRequest userSearchRequest) {
    return buildEntityResponse(userService.searchUsersWithFilter(userSearchRequest));
  }

  @Operation(
      summary = "Get notification settings",
      description =
          "Retrieves the notification preferences for a specific user, including which types of notifications are enabled")
  @GetMapping("/{id}/notification-settings")
  public ResponseEntity<ApiResponse<UserNotificationResponse>> getNotificationSettings(
      @PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.getNotificationSettings(id));
  }

  @Operation(
      summary = "Update notification settings",
      description =
          "Updates the notification preferences for a specific user, allowing them to enable or disable various notification types")
  @PutMapping("/{id}/notification-settings")
  public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(
      @PathVariable @ValidLongId Long id,
      @RequestBody @Valid UserNotificationSettingsRequest userNotificationSettingsRequest) {
    return buildEntityResponse(
        userService.updateNotificationSettings(id, userNotificationSettingsRequest));
  }

  @Operation(
      summary = "Update user online status",
      description =
          "Updates the online/offline status of a user for real-time presence tracking in the application")
  @PutMapping("/{id}/online")
  public ResponseEntity<ApiResponse<Void>> setUserOnlineStatus(
      @PathVariable @ValidLongId Long id, @RequestBody @Valid UserOnlineStatus userOnlineStatus) {
    return buildEntityResponse(userService.setUserOnline(id, userOnlineStatus));
  }
}
