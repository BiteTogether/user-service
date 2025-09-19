package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;

import com.bitetogether.common.configuration.security.JwtProperties;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.ErrorCode;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.response.LoginResponse;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.AuthService;
import com.bitetogether.user.service.JwtService;
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

  UserRepository userRepository;
  JwtService jwtService;
  JwtProperties jwtProperties;
  private final PasswordEncoder passwordEncoder;

  @Override
  public ApiResponse<LoginResponse> login(LoginRequest loginRequest) {
    User user = validateUserLogin(loginRequest);

    String accessToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    LoginResponse loginResponse = createLoginResponse(accessToken, refreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Login successful", loginResponse);
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

  private LoginResponse createLoginResponse(String accessToken, String refreshToken) {
    LoginResponse response = new LoginResponse();
    response.setAccessToken(accessToken);
    response.setRefreshToken(refreshToken);
    response.setExpiresIn(jwtProperties.getExpiration() / 1000);
    response.setRefreshExpiresIn(jwtProperties.getExpiration() / 1000);
    response.setSessionState(java.util.UUID.randomUUID().toString());

    return response;
  }
}
