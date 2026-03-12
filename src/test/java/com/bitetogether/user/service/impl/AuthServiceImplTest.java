package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.configuration.security.JwtProperties;
import com.bitetogether.user.dto.auth.request.FirebaseTokenRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.request.RegisterRequest;
import com.bitetogether.user.dto.auth.response.RefreshTokenReponse;
import com.bitetogether.user.dto.auth.response.TokenResponse;
import com.bitetogether.user.dto.user.request.SaveDeviceTokenRequest;
import com.bitetogether.user.dto.user.response.SaveDeviceTokenResponse;
import com.bitetogether.user.model.RefreshToken;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.FirebaseAuthService;
import com.bitetogether.user.service.JwtService;
import com.bitetogether.user.util.AuthUtils;
import com.google.firebase.auth.FirebaseToken;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
  "java:S2699",
  "java:S6073"
}) // Sonar: assertions present, unboxing warnings are false positives
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private JwtService jwtService;

  @Mock private JwtProperties jwtProperties;

  @Mock private FirebaseAuthService firebaseAuthService;

  @InjectMocks private AuthServiceImpl authService;

  @Mock private FirebaseToken firebaseToken;

  private User testUser;
  private FirebaseTokenRequest firebaseTokenRequest;
  private RegisterRequest registerRequest;
  private RefreshTokenRequest refreshTokenRequest;
  private SaveDeviceTokenRequest saveDeviceTokenRequest;
  private RefreshToken refreshToken;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .firebaseUid("firebase-uid-123")
            .fullName("Test User")
            .phoneNumber("1234567890")
            .role(Role.USER.name())
            .build();

    firebaseTokenRequest = new FirebaseTokenRequest();
    firebaseTokenRequest.setIdToken("valid-firebase-token");

    registerRequest = new RegisterRequest();
    registerRequest.setIdToken("valid-firebase-token");
    registerRequest.setUsername("newuser");
    registerRequest.setFullName("New User");

    refreshTokenRequest = new RefreshTokenRequest();
    refreshTokenRequest.setRefreshToken("validRefreshToken");

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
  void firebaseLogin_WithValidTokenAndExistingUser_ReturnsTokenResponse() {
    String accessToken = "access-token";
    String refreshTokenValue = "refresh-token";
    Long expiration = 3600000L;
    Long refreshExpiration = 604800000L;
    String firebaseUid = "firebase-uid-123";
    String phoneNumber = "1234567890";

    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", phoneNumber);

    when(firebaseAuthService.verifyIdToken(firebaseTokenRequest.getIdToken()))
        .thenReturn(firebaseToken);
    when(firebaseToken.getUid()).thenReturn(firebaseUid);
    when(firebaseToken.getClaims()).thenReturn(claims);
    when(userRepository.findByFirebaseUid(firebaseUid)).thenReturn(Optional.of(testUser));
    when(jwtService.generateToken(any(User.class), anyString())).thenReturn(accessToken);
    when(jwtService.generateRefreshToken(any(User.class), anyString()))
        .thenReturn(refreshTokenValue);
    when(jwtProperties.getExpiration()).thenReturn(expiration);
    when(jwtProperties.getRefreshExpiration()).thenReturn(refreshExpiration);

    ApiResponseDTO<TokenResponse> response = authService.firebaseLogin(firebaseTokenRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    assertEquals("Login successfully", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(accessToken, response.getData().getAccessToken());
    assertEquals(refreshTokenValue, response.getData().getRefreshToken());
    assertEquals(expiration, response.getData().getExpiresIn());
    assertEquals(refreshExpiration, response.getData().getRefreshExpiresIn());
    assertNotNull(response.getData().getSessionState());
    verify(firebaseAuthService).verifyIdToken(firebaseTokenRequest.getIdToken());
    verify(userRepository).findByFirebaseUid(firebaseUid);
    verify(jwtService).generateToken(any(User.class), anyString());
    verify(jwtService).generateRefreshToken(any(User.class), anyString());
  }

  @Test
  void firebaseLogin_WithNonExistentUser_ThrowsAppException() {
    String firebaseUid = "firebase-uid-123";
    String phoneNumber = "9999999999";

    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", phoneNumber);

    when(firebaseAuthService.verifyIdToken(firebaseTokenRequest.getIdToken()))
        .thenReturn(firebaseToken);
    when(firebaseToken.getUid()).thenReturn(firebaseUid);
    when(firebaseToken.getClaims()).thenReturn(claims);
    when(userRepository.findByFirebaseUid(firebaseUid)).thenReturn(Optional.empty());
    when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

    assertThrows(AppException.class, () -> authService.firebaseLogin(firebaseTokenRequest));

    verify(firebaseAuthService).verifyIdToken(firebaseTokenRequest.getIdToken());
    verify(jwtService, never()).generateToken(any(User.class), anyString());
  }

  @Test
  void register_WithValidRequest_ReturnsTokenResponse() {
    String accessToken = "access-token";
    String refreshTokenValue = "refresh-token";
    Long expiration = 3600000L;
    Long refreshExpiration = 604800000L;
    String firebaseUid = "new-firebase-uid";
    String phoneNumber = "0987654321";

    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", phoneNumber);

    when(firebaseAuthService.verifyIdToken(registerRequest.getIdToken())).thenReturn(firebaseToken);
    when(firebaseToken.getUid()).thenReturn(firebaseUid);
    when(firebaseToken.getClaims()).thenReturn(claims);
    when(userRepository.findByFirebaseUid(firebaseUid)).thenReturn(Optional.empty());
    when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtService.generateToken(any(User.class), anyString())).thenReturn(accessToken);
    when(jwtService.generateRefreshToken(any(User.class), anyString()))
        .thenReturn(refreshTokenValue);
    when(jwtProperties.getExpiration()).thenReturn(expiration);
    when(jwtProperties.getRefreshExpiration()).thenReturn(refreshExpiration);

    ApiResponseDTO<TokenResponse> response = authService.register(registerRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    assertEquals("User registered successfully", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(accessToken, response.getData().getAccessToken());
    assertEquals(refreshTokenValue, response.getData().getRefreshToken());
    verify(firebaseAuthService).verifyIdToken(registerRequest.getIdToken());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_WithExistingFirebaseUid_ThrowsAppException() {
    String firebaseUid = "existing-firebase-uid";
    String phoneNumber = "1234567890";

    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", phoneNumber);

    when(firebaseAuthService.verifyIdToken(registerRequest.getIdToken())).thenReturn(firebaseToken);
    when(firebaseToken.getUid()).thenReturn(firebaseUid);
    when(firebaseToken.getClaims()).thenReturn(claims);
    when(userRepository.findByFirebaseUid(firebaseUid)).thenReturn(Optional.of(testUser));

    assertThrows(AppException.class, () -> authService.register(registerRequest));

    verify(firebaseAuthService).verifyIdToken(registerRequest.getIdToken());
    verify(userRepository, never()).save(any(User.class));
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

      ApiResponseDTO<Void> response = authService.logOut();

      assertNotNull(response);
      assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
      assertEquals("Log out successfully", response.getMessage());
      verify(jwtService).extractRefreshJti(accessToken);
      verify(refreshTokenRepository).findById(refreshJti);
      verify(refreshTokenRepository).delete(refreshToken);
    }
  }

  @Test
  void logOut_WithNonExistentRefreshToken_ThrowsAppException() {
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
  void logOut_WithMismatchedUserId_ThrowsAppException() {
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
    String username = "testuser";
    String refreshJti = "refresh-jti-123";
    String newAccessToken = "new-access-token";
    Long expiration = 3600000L;

    when(jwtService.extractUsername(refreshTokenRequest.getRefreshToken())).thenReturn(username);
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
    when(jwtService.extractJti(refreshTokenRequest.getRefreshToken())).thenReturn(refreshJti);
    when(jwtService.generateToken(testUser, refreshJti)).thenReturn(newAccessToken);
    when(jwtProperties.getExpiration()).thenReturn(expiration);

    ApiResponseDTO<RefreshTokenReponse> response = authService.refreshToken(refreshTokenRequest);

    assertNotNull(response);
    assertEquals(ApiResponseStatus.SUCCESS.getCode(), response.getStatus());
    assertEquals("Token refreshed successfully", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(newAccessToken, response.getData().getAccessToken());
    assertEquals(expiration, response.getData().getExpiresIn());
    assertNotNull(response.getData().getSessionState());
    verify(jwtService).extractUsername(refreshTokenRequest.getRefreshToken());
    verify(userRepository).findByUsername(username);
    verify(jwtService).extractJti(refreshTokenRequest.getRefreshToken());
    verify(jwtService).generateToken(testUser, refreshJti);
  }

  @Test
  void refreshToken_WithNonExistentUser_ThrowsAppException() {
    String username = "nonexistent";

    when(jwtService.extractUsername(refreshTokenRequest.getRefreshToken())).thenReturn(username);
    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(AppException.class, () -> authService.refreshToken(refreshTokenRequest));

    verify(jwtService).extractUsername(refreshTokenRequest.getRefreshToken());
    verify(userRepository).findByUsername(username);
    verify(jwtService, never()).generateToken(any(), anyString());
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

      ApiResponseDTO<Void> response = authService.saveDeviceToken(saveDeviceTokenRequest);

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

      ApiResponseDTO<SaveDeviceTokenResponse> response = authService.getDeviceToken();

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
