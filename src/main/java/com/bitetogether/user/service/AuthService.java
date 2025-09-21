package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import jakarta.validation.Valid;

public interface AuthService {
  ApiResponse<TokenResponse> login(LoginRequest loginRequest);

  ApiResponse<TokenResponse> refreshToken(RefreshTokenRequest refreshTokenRequest);

    ApiResponse<Long> register(@Valid CreateUserRequest createUserRequest);
}
