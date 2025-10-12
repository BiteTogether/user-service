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
public class AuthController {
  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> logIn(
      @Valid @RequestBody LoginRequest loginRequest) {
    return buildEntityResponse(authService.logIn(loginRequest));
  }

  @DeleteMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logOut() {
    return buildEntityResponse(authService.logOut());
  }

  @PostMapping("/tokens/refresh")
  public ResponseEntity<ApiResponse<RefreshTokenReponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
    return buildEntityResponse(authService.refreshToken(refreshTokenRequest));
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<Long>> register(
      @Valid @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(authService.register(createUserRequest));
  }

  @PostMapping("/tokens/device-token")
  public ResponseEntity<ApiResponse<Void>> saveDeviceToken(
      @Valid @RequestBody SaveDeviceTokenRequest saveDeviceTokenRequest) {
    return buildEntityResponse(authService.saveDeviceToken(saveDeviceTokenRequest));
  }

  @GetMapping("/tokens/device-token")
  public ResponseEntity<ApiResponse<SaveDeviceTokenResponse>> getDeviceToken() {
    return buildEntityResponse(authService.getDeviceToken());
  }
}
