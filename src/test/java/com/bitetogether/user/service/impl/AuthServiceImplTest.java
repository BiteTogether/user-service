package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.configuration.security.JwtProperties;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.SaveDeviceTokenRequest;
import com.bitetogether.user.dto.user.response.SaveDeviceTokenResponse;
import com.bitetogether.user.model.RefreshToken;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.JwtService;
import com.bitetogether.user.service.UserService;
import com.bitetogether.user.util.AuthUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private UserService userService;

  @Mock private UserRepository userRepository;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private JwtService jwtService;

  @Mock private JwtProperties jwtProperties;

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthServiceImpl authService;

  private User testUser;
  private LoginRequest loginRequest;
  private RefreshTokenRequest refreshTokenRequest;
  private CreateUserRequest createUserRequest;
  private SaveDeviceTokenRequest saveDeviceTokenRequest;
  private RefreshToken refreshToken;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encodedPassword")
            .fullName("Test User")
            .phoneNumber("1234567890")
            .role(Role.USER.name())
            .build();

    loginRequest = new LoginRequest("test@example.com", "password123");

    refreshTokenRequest = new RefreshTokenRequest();
    refreshTokenRequest.setRefreshToken("validRefreshToken");

    createUserRequest = new CreateUserRequest();

    saveDeviceTokenRequest = new SaveDeviceTokenRequest();
    saveDeviceTokenRequest.setDeviceToken("device-token-123");

    refreshToken = new RefreshToken();
    refreshToken.setJti("refresh-jti-123");
    refreshToken.setUser(testUser);
    refreshToken.setIssuedAt(LocalDateTime.now());
    refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
    refreshToken.setDeviceToken("");
  }

  @Test
  void logIn_WithValidCredentials_ReturnsTokenResponse() {
    String accessToken = "access-token";
    String refreshTokenValue = "refresh-token";
    Long expiration = 3600000L;
    Long refreshExpiration = 604800000L;

    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
        .thenReturn(true);
    when(jwtService.generateToken(eq(testUser), anyString())).thenReturn(accessToken);
    when(jwtService.generateRefreshToken(eq(testUser), anyString())).thenReturn(refreshTokenValue);
    when(jwtProperties.getExpiration()).thenReturn(expiration);
    when(jwtProperties.getRefreshExpiration()).thenReturn(refreshExpiration);

    ApiResponse<TokenResponse> response = authService.logIn(loginRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    assertEquals("Log in successfully", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(accessToken, response.getData().getAccessToken());
    assertEquals(refreshTokenValue, response.getData().getRefreshToken());
    assertEquals(expiration, response.getData().getExpiresIn());
    assertEquals(refreshExpiration, response.getData().getRefreshExpiresIn());
    assertNotNull(response.getData().getSessionState());

    verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), testUser.getPassword());
    verify(jwtService, times(1)).generateToken(eq(testUser), anyString());
    verify(jwtService, times(1)).generateRefreshToken(eq(testUser), anyString());
  }

  @Test
  void logIn_WithNonExistentUser_ThrowsUnauthorizedException() {
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

    assertThrows(AppException.class, () -> authService.logIn(loginRequest));

    verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(jwtService, never()).generateToken(any(), anyString());
  }

  @Test
  void logIn_WithInvalidPassword_ThrowsUnauthorizedException() {
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
        .thenReturn(false);

    assertThrows(AppException.class, () -> authService.logIn(loginRequest));

    verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), testUser.getPassword());
    verify(jwtService, never()).generateToken(any(), anyString());
  }

  @Test
  void logOut_WithValidToken_DeletesRefreshToken() {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));

      ApiResponse<Void> response = authService.logOut();

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Log out successfully", response.getMessage());

      verify(jwtService, times(1)).extractRefreshJti(accessToken);
      verify(refreshTokenRepository, times(1)).findById(refreshJti);
      verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }
  }

  @Test
  void logOut_WithNonExistentRefreshToken_ThrowsNotFoundException() {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "non-existent-jti";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.empty());

      assertThrows(AppException.class, () -> authService.logOut());

      verify(refreshTokenRepository, never()).delete(any());
    }
  }

  @Test
  void logOut_WithMismatchedUserId_ThrowsNotFoundException() {
    Long currentUserId = 2L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));

      assertThrows(AppException.class, () -> authService.logOut());

      verify(refreshTokenRepository, never()).delete(any());
    }
  }

  @Test
  void refreshToken_WithValidToken_ReturnsNewAccessToken() {
    String email = "test@example.com";
    String refreshJti = "refresh-jti-123";
    String newAccessToken = "new-access-token";
    Long expiration = 3600000L;

    when(jwtService.extractEmail(refreshTokenRequest.getRefreshToken())).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
    when(jwtService.extractJti(refreshTokenRequest.getRefreshToken())).thenReturn(refreshJti);
    when(jwtService.generateToken(testUser, refreshJti)).thenReturn(newAccessToken);
    when(jwtProperties.getExpiration()).thenReturn(expiration);

    ApiResponse<RefreshTokenReponse> response = authService.refreshToken(refreshTokenRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    assertEquals("Token refreshed successfully", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(newAccessToken, response.getData().getAccessToken());
    assertEquals(expiration, response.getData().getExpiresIn());
    assertNotNull(response.getData().getSessionState());

    verify(jwtService, times(1)).extractEmail(refreshTokenRequest.getRefreshToken());
    verify(userRepository, times(1)).findByEmail(email);
    verify(jwtService, times(1)).extractJti(refreshTokenRequest.getRefreshToken());
    verify(jwtService, times(1)).generateToken(testUser, refreshJti);
  }

  @Test
  void refreshToken_WithNonExistentUser_ThrowsNotFoundException() {
    String email = "nonexistent@example.com";

    when(jwtService.extractEmail(refreshTokenRequest.getRefreshToken())).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    assertThrows(AppException.class, () -> authService.refreshToken(refreshTokenRequest));

    verify(jwtService, times(1)).extractEmail(refreshTokenRequest.getRefreshToken());
    verify(userRepository, times(1)).findByEmail(email);
    verify(jwtService, never()).generateToken(any(), anyString());
  }

  @Test
  void register_WithValidRequest_ReturnsUserId() {
    ApiResponse<Long> expectedResponse =
        ApiResponse.<Long>builder()
            .status(ApiResponseStatus.SUCCESS.getCode())
            .message("User created successfully")
            .data(1L)
            .build();

    when(userService.createUser(createUserRequest)).thenReturn(expectedResponse);

    ApiResponse<Long> response = authService.register(createUserRequest);

    assertNotNull(response);
    assertEquals(expectedResponse.getStatus(), response.getStatus());
    assertEquals(expectedResponse.getMessage(), response.getMessage());
    assertEquals(expectedResponse.getData(), response.getData());
    assertEquals(Role.USER.name(), createUserRequest.getRole());

    verify(userService, times(1)).createUser(createUserRequest);
  }

  @Test
  void saveDeviceToken_WithValidRequest_UpdatesDeviceToken() {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";
    String deviceToken = "device-token-123";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));
      when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

      ApiResponse<Void> response = authService.saveDeviceToken(saveDeviceTokenRequest);

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals(
          "User's device information has been updated successfully", response.getMessage());
      assertEquals(deviceToken, refreshToken.getDeviceToken());

      verify(jwtService, times(1)).extractRefreshJti(accessToken);
      verify(refreshTokenRepository, times(1)).findById(refreshJti);
      verify(refreshTokenRepository, times(1)).save(refreshToken);
    }
  }

  @Test
  void saveDeviceToken_WithNonExistentRefreshToken_ThrowsNotFoundException() {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "non-existent-jti";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.empty());

      assertThrows(AppException.class, () -> authService.saveDeviceToken(saveDeviceTokenRequest));

      verify(refreshTokenRepository, never()).save(any());
    }
  }

  @Test
  void saveDeviceToken_WithMismatchedUserId_ThrowsNotFoundException() {
    Long currentUserId = 2L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));

      assertThrows(AppException.class, () -> authService.saveDeviceToken(saveDeviceTokenRequest));

      verify(refreshTokenRepository, never()).save(any());
    }
  }

  @Test
  void getDeviceToken_WithValidToken_ReturnsDeviceToken() {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";
    String deviceToken = "device-token-123";

    refreshToken.setDeviceToken(deviceToken);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));

      ApiResponse<SaveDeviceTokenResponse> response = authService.getDeviceToken();

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals(
          "User's device information has been fetched successfully", response.getMessage());
      assertNotNull(response.getData());
      assertEquals(deviceToken, response.getData().getDeviceToken());

      verify(jwtService, times(1)).extractRefreshJti(accessToken);
      verify(refreshTokenRepository, times(1)).findById(refreshJti);
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  void getDeviceToken_WithInvalidDeviceToken_ThrowsNotFoundException(String deviceToken) {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";

    refreshToken.setDeviceToken(deviceToken);

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));

      assertThrows(AppException.class, () -> authService.getDeviceToken());
    }
  }

  @Test
  void getDeviceToken_WithNonExistentRefreshToken_ThrowsNotFoundException() {
    Long currentUserId = 1L;
    String accessToken = "valid-access-token";
    String refreshJti = "non-existent-jti";

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.empty());

      assertThrows(AppException.class, () -> authService.getDeviceToken());
    }
  }

  @Test
  void getDeviceToken_WithMismatchedUserId_ThrowsNotFoundException() {
    Long currentUserId = 2L;
    String accessToken = "valid-access-token";
    String refreshJti = "refresh-jti-123";

    refreshToken.setDeviceToken("device-token-123");

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
      authUtilsMock.when(AuthUtils::getAccessTokenFromHeader).thenReturn(accessToken);

      when(jwtService.extractRefreshJti(accessToken)).thenReturn(refreshJti);
      when(refreshTokenRepository.findById(refreshJti)).thenReturn(Optional.of(refreshToken));

      assertThrows(AppException.class, () -> authService.getDeviceToken());
    }
  }
}
