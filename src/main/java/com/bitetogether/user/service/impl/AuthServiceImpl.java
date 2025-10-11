package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.common.util.SecurityUtils.getAccessTokenFromHeader;
import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;

import com.bitetogether.common.configuration.security.JwtProperties;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.SaveDeviceTokenRequest;
import com.bitetogether.user.dto.user.response.SaveDeviceTokenResponse;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.RefreshToken;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.AuthService;
import com.bitetogether.user.service.JwtService;
import com.bitetogether.user.service.UserService;
import com.bitetogether.user.util.UserHelper;
import java.util.Objects;
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
  UserHelper userHelper;
  UserRepository userRepository;
  RefreshTokenRepository refreshTokenRepository;
  JwtService jwtService;
  JwtProperties jwtProperties;
  PasswordEncoder passwordEncoder;

  @Override
  public ApiResponse<TokenResponse> logIn(LoginRequest loginRequest) {
    User user = validateUserLogin(loginRequest);

    String refreshTokenJti = java.util.UUID.randomUUID().toString();
    String accessToken = jwtService.generateToken(user, refreshTokenJti);
    String refreshToken = jwtService.generateRefreshToken(user, refreshTokenJti);

    TokenResponse loginResponse = createTokenResponse(accessToken, refreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Log in successfully", loginResponse);
  }

  @Override
  public ApiResponse<Void> logOut() {
    Long currentUserId = getCurrentUserId();

    String accessToken = getAccessTokenFromHeader();
    String refreshJti = jwtService.extractRefreshJti(accessToken);

    RefreshToken refreshToken = validateRefreshToken(refreshJti, currentUserId);

    refreshTokenRepository.delete(refreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Log out successfully", null);
  }

  @Override
  public ApiResponse<RefreshTokenReponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
    String refreshToken = refreshTokenRequest.getRefreshToken();

    String email = jwtService.extractEmail(refreshToken);
    User user = findUserByEmail(email);

    String refreshTokenJti = jwtService.extractJti(refreshToken);
    String newAccessToken = jwtService.generateToken(user, refreshTokenJti);

    RefreshTokenReponse response = createRefreshTokenResponse(newAccessToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Token refreshed successfully", response);
  }

  @Override
  public ApiResponse<Long> register(CreateUserRequest createUserRequest) {
    createUserRequest.setRole(Role.USER.name());
    return userService.createUser(createUserRequest);
  }

  @Override
  public ApiResponse<Void> saveDeviceToken(SaveDeviceTokenRequest requestDto) {
    Long currentUserId = getCurrentUserId();

    String accessToken = getAccessTokenFromHeader();
    String refreshJti = jwtService.extractRefreshJti(accessToken);

    RefreshToken refreshToken = validateRefreshToken(refreshJti, currentUserId);
    String deviceToken = requestDto.getDeviceToken();

    refreshToken.setDeviceToken(deviceToken);
    refreshTokenRepository.save(refreshToken);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User's device information has been updated successfully", null);
  }

  @Override
  public ApiResponse<SaveDeviceTokenResponse> getDeviceToken() {
    Long currentUserId = getCurrentUserId();

    String accessToken = getAccessTokenFromHeader();
    String refreshJti = jwtService.extractRefreshJti(accessToken);

    RefreshToken refreshToken = validateRefreshToken(refreshJti, currentUserId);
    String deviceToken = refreshToken.getDeviceToken();

    if (deviceToken == null || deviceToken.isEmpty()) {
      throw new AppException(ErrorCode.DEVICE_TOKEN_NOT_FOUND);
    }

    SaveDeviceTokenResponse responseDto = new SaveDeviceTokenResponse();
    responseDto.setDeviceToken(deviceToken);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "User's device information has been fetched successfully",
        responseDto);
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
        .expiresIn(jwtProperties.getExpiration())
        .refreshExpiresIn(jwtProperties.getRefreshExpiration())
        .sessionState(java.util.UUID.randomUUID().toString())
        .build();
  }

  private RefreshTokenReponse createRefreshTokenResponse(String accessToken) {
    return RefreshTokenReponse.builder()
        .accessToken(accessToken)
        .expiresIn(jwtProperties.getExpiration())
        .sessionState(java.util.UUID.randomUUID().toString())
        .build();
  }

  private RefreshToken validateRefreshToken(String refreshJti, Long currentUserId) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findById(refreshJti)
            .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

    if (!Objects.equals(refreshToken.getUser().getId(), currentUserId)) {
      throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    return refreshToken;
  }
}
