package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;
import static com.bitetogether.common.util.Constants.PREFIX_REQUEST_MAPPING_AUTH;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest) {
    return buildEntityResponse(authService.login(loginRequest));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
    return buildEntityResponse(authService.refreshToken(refreshTokenRequest));
  }

  @PostMapping()
  public ResponseEntity<ApiResponse<Long>> register(
      @Valid @RequestBody CreateUserRequest createUserRequest) {
    return buildEntityResponse(authService.register(createUserRequest));
  }
}
