package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.SaveDeviceTokenRequest;
import com.bitetogether.user.dto.user.response.SaveDeviceTokenResponse;
import jakarta.validation.Valid;

public interface AuthService {
  ApiResponse<TokenResponse> logIn(LoginRequest loginRequest);

  ApiResponse<Void> logOut();

  ApiResponse<RefreshTokenReponse> refreshToken(RefreshTokenRequest refreshTokenRequest);

  ApiResponse<Long> register(@Valid CreateUserRequest createUserRequest);

  ApiResponse<Void> saveDeviceToken(SaveDeviceTokenRequest requestDto);

  ApiResponse<SaveDeviceTokenResponse> getDeviceToken();
}
