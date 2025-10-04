package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;

import com.bitetogether.common.configuration.security.JwtProperties;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.AuthService;
import com.bitetogether.user.service.JwtService;
import com.bitetogether.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
  UserService userService;
  UserRepository userRepository;
  JwtService jwtService;
  JwtProperties jwtProperties;
  PasswordEncoder passwordEncoder;

  @Override
  public ApiResponse<TokenResponse> login(LoginRequest loginRequest) {
    User user = validateUserLogin(loginRequest);

    String accessToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    TokenResponse loginResponse = createTokenResponse(accessToken, refreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Login successful", loginResponse);
  }

  @Override
  public ApiResponse<TokenResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
    String refreshToken = refreshTokenRequest.getRefreshToken();

    String email = jwtService.extractEmail(refreshToken);
    User user = findUserByEmail(email);

    String newAccessToken = jwtService.generateToken(user);
    String newRefreshToken = jwtService.generateRefreshToken(user);

    TokenResponse response = createTokenResponse(newAccessToken, newRefreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Token refreshed successfully", response);
  }

  @Override
  public ApiResponse<Long> register(CreateUserRequest createUserRequest) {
    createUserRequest.setRole(Role.USER.name());
    return userService.createUser(createUserRequest);
  }

  private User validateUserLogin(LoginRequest loginRequest) {
    User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);

    if (user == null) {
      throw new AppException(ErrorCode.UNAUTHORIZED_LOGIN);
    }

    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      throw new AppException(ErrorCode.UNAUTHORIZED_LOGIN);
    }

    return user;
  }

  private User findUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private TokenResponse createTokenResponse(String accessToken, String refreshToken) {
    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(jwtProperties.getExpiration() / 1000)
        .refreshExpiresIn(jwtProperties.getRefreshExpiration() / 1000)
        .sessionState(java.util.UUID.randomUUID().toString())
        .build();
  }
}
