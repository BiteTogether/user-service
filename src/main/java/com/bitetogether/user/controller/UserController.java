package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_USER;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.validation.ValidLongId;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdatePhoneRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserState;
import com.bitetogether.user.dto.user.request.UserNotificationSettingsRequest;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.request.ValidateUserCriteriaRequest;
import com.bitetogether.user.dto.user.response.ListUserDetailsResponse;
import com.bitetogether.user.dto.user.response.UpdatePhoneResponse;
import com.bitetogether.user.dto.user.response.UserDetailsResponse;
import com.bitetogether.user.dto.user.response.UserGetByIdResponse;
import com.bitetogether.user.dto.user.response.UserNotificationResponse;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.dto.user.response.UserSearchResponse;
import com.bitetogether.user.dto.user.response.UserStateResponse;
import com.bitetogether.user.dto.user.response.ValidateUserCriteriaResponse;
import com.bitetogether.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
  public ResponseEntity<ApiResponseDTO<Long>> createUser(
      @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(userService.createUser(createUserRequest));
  }

  @Operation(
      summary = "Update user profile",
      description =
          "Updates the profile information of a specific user. Users can update their own profile or admins can update any user's profile")
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<UserResponse>> updateUser(
      @PathVariable @ValidLongId Long id, @RequestBody @Valid UpdateUserRequest updateUserRequest) {
    return buildEntityResponse(userService.updateUser(id, updateUserRequest));
  }

  @Operation(
      summary = "Delete user",
      description =
          "Deletes a user account permanently. This action removes all user data and cannot be undone")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<String>> deleteUser(@PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.deleteUser(id));
  }

  @Operation(
      summary = "Get current user profile",
      description =
          "Retrieves the detailed profile information of the currently authenticated user")
  @GetMapping("/me")
  public ResponseEntity<ApiResponseDTO<UserDetailsResponse>> getCurrentUser() {
    return buildEntityResponse(userService.getCurrentUser());
  }

  @Operation(
      summary = "Get user by ID",
      description = "Retrieves the profile information of a specific user by their user ID")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<UserGetByIdResponse>> getUserById(
      @PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.getUserById(id));
  }

  @Operation(
      summary = "Get list of user details",
      description = "Retrieves detailed information for a list of users based on their IDs")
  @GetMapping
  public ResponseEntity<ApiResponseDTO<ListUserDetailsResponse>> getListUserDetails(
      @RequestParam List<@Positive Long> userIds) {
    return buildEntityResponse(userService.getListUser(userIds));
  }

  @Operation(
      summary = "Search users",
      description =
          "Searches for users based on various filters such as name, email, or other criteria. Returns paginated results")
  @PostMapping("/search")
  public ResponseEntity<ApiResponseDTO<UserSearchResponse>> searchUsersWithFilter(
      @RequestBody @Valid UserSearchRequest userSearchRequest) {
    return buildEntityResponse(userService.searchUsersWithFilter(userSearchRequest));
  }

  @Operation(
      summary = "Get notification settings",
      description =
          "Retrieves the notification preferences for a specific user, including which types of notifications are enabled")
  @GetMapping("/{id}/notification-settings")
  public ResponseEntity<ApiResponseDTO<UserNotificationResponse>> getNotificationSettings(
      @PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.getNotificationSettings(id));
  }

  @Operation(
      summary = "Update notification settings",
      description =
          "Updates the notification preferences for a specific user, allowing them to enable or disable various notification types")
  @PutMapping("/{id}/notification-settings")
  public ResponseEntity<ApiResponseDTO<Void>> updateNotificationSettings(
      @PathVariable @ValidLongId Long id,
      @RequestBody @Valid UserNotificationSettingsRequest userNotificationSettingsRequest) {
    return buildEntityResponse(
        userService.updateNotificationSettings(id, userNotificationSettingsRequest));
  }

  @Operation(
      summary = "Upload user avatar",
      description =
          "Uploads a new avatar image for the specified user. The image will be stored in Firebase Storage and the URL will be saved to the user profile. Supports JPEG, PNG, GIF, and WebP formats with a maximum size of 5MB")
  @PostMapping(value = "/{id}/avatar", consumes = "multipart/form-data")
  public ResponseEntity<ApiResponseDTO<String>> uploadAvatar(
      @PathVariable @ValidLongId Long id, @RequestParam("file") MultipartFile file) {
    return buildEntityResponse(userService.uploadAvatar(id, file));
  }

  @Operation(
      summary = "Delete user avatar",
      description =
          "Deletes the avatar image of the specified user. The image will be removed from Firebase Storage and the avatar URL will be cleared from the user profile")
  @DeleteMapping("/{id}/avatar")
  public ResponseEntity<ApiResponseDTO<Void>> deleteAvatar(@PathVariable @ValidLongId Long id) {
    return buildEntityResponse(userService.deleteAvatar(id));
  }

  @Operation(
      summary = "Update phone number",
      description =
          "Updates the phone number of the specified user using Firebase ID Token (from phone OTP verification). "
              + "This endpoint verifies the new phone number through Firebase Authentication and updates both the phone number and Firebase UID. "
              + "The phone number must be unique and verified through OTP")
  @PutMapping("/phone")
  public ResponseEntity<ApiResponseDTO<UpdatePhoneResponse>> updatePhone(
      @RequestBody @Valid UpdatePhoneRequest updatePhoneRequest) {
    return buildEntityResponse(userService.updatePhone(updatePhoneRequest));
  }

  @Operation(
      summary = "Validate user criteria",
      description =
          "Validates user input criteria before registration. "
              + "For USERNAME: Checks format (6-20 chars, letters, numbers, dots, underscores) and availability. "
              + "For PHONE: Checks if phone number is already registered in the system")
  @PostMapping("/validate")
  public ResponseEntity<ApiResponseDTO<ValidateUserCriteriaResponse>> validateUserCriteria(
      @RequestBody @Valid ValidateUserCriteriaRequest criteria) {
    return buildEntityResponse(userService.validateUserCriteria(criteria));
  }

  @Operation(
      summary = "Update user state",
      description =
          "Updates a user's foreground/background/offline state for real-time presence tracking")
  @PatchMapping("/{id}/state")
  public ResponseEntity<ApiResponseDTO<Void>> updateUserState(
      @PathVariable @ValidLongId Long id, @RequestBody @Valid UpdateUserState updateUserState) {
    return buildEntityResponse(userService.updateUserState(id, updateUserState));
  }

  @Operation(
      summary = "Get current user state",
      description =
          "Retrieves the current online status and last seen time of the authenticated user. "
              + "Returns the state from cache if available, otherwise fetches from database")
  @GetMapping("/state")
  public ResponseEntity<ApiResponseDTO<UserStateResponse>> getCurrentUserState() {
    return buildEntityResponse(userService.getCurrentUserState());
  }
}
