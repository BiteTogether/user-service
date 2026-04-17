package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.user.dto.auth.request.FirebaseTokenRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.request.RegisterRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;

public interface AuthService {

  ApiResponseDTO<TokenResponse> firebaseLogin(FirebaseTokenRequest firebaseLoginRequest);

  ApiResponseDTO<TokenResponse> register(RegisterRequest registerRequest);

  ApiResponseDTO<Void> logOut();

  ApiResponseDTO<RefreshTokenReponse> refreshToken(RefreshTokenRequest refreshTokenRequest);
}
