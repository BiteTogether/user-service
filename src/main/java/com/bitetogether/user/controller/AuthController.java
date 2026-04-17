package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_AUTH;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.user.dto.auth.request.FirebaseTokenRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.request.RegisterRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        "APIs for user authentication with Firebase Phone OTP, token management, and device token handling")
public class AuthController {
  private final AuthService authService;

  @Operation(
      summary = "Login existing user",
      description =
          "Authenticates an existing user using Firebase ID Token (from phone OTP verification). "
              + "Returns access token and refresh token for subsequent API calls. "
              + "If user doesn't exist, returns error - user should register first")
  @PostMapping("/login")
  public ResponseEntity<ApiResponseDTO<TokenResponse>> login(
      @Valid @RequestBody FirebaseTokenRequest firebaseLoginRequest) {
    return buildEntityResponse(authService.firebaseLogin(firebaseLoginRequest));
  }

  @Operation(
      summary = "Register new user",
      description =
          "Registers a new user using Firebase ID Token (from phone OTP verification). "
              + "User can optionally provide username and full name. "
              + "Returns access token and refresh token after successful registration")
  @PostMapping("/register")
  public ResponseEntity<ApiResponseDTO<TokenResponse>> register(
      @Valid @RequestBody RegisterRequest registerRequest) {
    return buildEntityResponse(authService.register(registerRequest));
  }

  @Operation(
      summary = "User logout",
      description =
          "Logs out the currently authenticated user by invalidating their refresh token and clearing the session")
  @DeleteMapping("/logout")
  public ResponseEntity<ApiResponseDTO<Void>> logOut() {
    return buildEntityResponse(authService.logOut());
  }

  @Operation(
      summary = "Refresh access token",
      description =
          "Generates a new access token using a valid refresh token when the current access token expires")
  @PostMapping("/tokens/refresh")
  public ResponseEntity<ApiResponseDTO<RefreshTokenReponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
    return buildEntityResponse(authService.refreshToken(refreshTokenRequest));
  }
}
