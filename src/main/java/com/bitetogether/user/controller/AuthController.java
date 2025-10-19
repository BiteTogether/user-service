package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_AUTH;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.SaveDeviceTokenRequest;
import com.bitetogether.user.dto.user.response.SaveDeviceTokenResponse;
import com.bitetogether.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(PREFIX_REQUEST_MAPPING_AUTH)
@Tag(
    name = "Authentication",
    description =
        "APIs for user authentication, registration, token management, and device token handling")
public class AuthController {
  private final AuthService authService;

  @Operation(
      summary = "User login",
      description =
          "Authenticates a user with email and password, returns access token and refresh token for subsequent API calls")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> logIn(
      @Valid @RequestBody LoginRequest loginRequest) {
    return buildEntityResponse(authService.logIn(loginRequest));
  }

  @Operation(
      summary = "User logout",
      description =
          "Logs out the currently authenticated user by invalidating their refresh token and clearing the session")
  @DeleteMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logOut() {
    return buildEntityResponse(authService.logOut());
  }

  @Operation(
      summary = "Refresh access token",
      description =
          "Generates a new access token using a valid refresh token when the current access token expires")
  @PostMapping("/tokens/refresh")
  public ResponseEntity<ApiResponse<RefreshTokenReponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
    return buildEntityResponse(authService.refreshToken(refreshTokenRequest));
  }

  @Operation(
      summary = "Register new user",
      description =
          "Creates a new user account with the provided information. Returns the newly created user's ID")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<Long>> register(
      @Valid @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(authService.register(createUserRequest));
  }

  @Operation(
      summary = "Save device token",
      description =
          "Saves or updates the device token for push notifications (FCM/APNS) for the authenticated user")
  @PostMapping("/tokens/device-token")
  public ResponseEntity<ApiResponse<Void>> saveDeviceToken(
      @Valid @RequestBody SaveDeviceTokenRequest saveDeviceTokenRequest) {
    return buildEntityResponse(authService.saveDeviceToken(saveDeviceTokenRequest));
  }

  @Operation(
      summary = "Get device token",
      description =
          "Retrieves the currently saved device token for push notifications for the authenticated user")
  @GetMapping("/tokens/device-token")
  public ResponseEntity<ApiResponse<SaveDeviceTokenResponse>> getDeviceToken() {
    return buildEntityResponse(authService.getDeviceToken());
  }
}
