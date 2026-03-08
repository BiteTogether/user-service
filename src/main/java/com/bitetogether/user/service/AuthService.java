package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.FirebaseTokenRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.request.RegisterRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.SaveDeviceTokenRequest;
import com.bitetogether.user.dto.user.response.SaveDeviceTokenResponse;

public interface AuthService {

  ApiResponse<TokenResponse> firebaseLogin(FirebaseTokenRequest firebaseLoginRequest);

  ApiResponse<TokenResponse> register(RegisterRequest registerRequest);

  ApiResponse<Void> logOut();

  ApiResponse<RefreshTokenReponse> refreshToken(RefreshTokenRequest refreshTokenRequest);

  ApiResponse<Void> saveDeviceToken(SaveDeviceTokenRequest requestDto);

  ApiResponse<SaveDeviceTokenResponse> getDeviceToken();
}
