package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.user.util.AuthUtils.getAccessTokenFromHeader;
import static com.bitetogether.user.util.AuthUtils.getCurrentUserId;

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
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.RefreshToken;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.AuthService;
import com.bitetogether.user.service.FirebaseAuthService;
import com.bitetogether.user.service.JwtService;
import com.bitetogether.user.util.UserEventPublisherHelper;
import com.google.firebase.auth.FirebaseToken;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
  UserRepository userRepository;
  RefreshTokenRepository refreshTokenRepository;
  JwtService jwtService;
  JwtProperties jwtProperties;
  FirebaseAuthService firebaseAuthService;
  UserEventPublisherHelper userEventPublisherHelper;

  private static final String PHONE_NUMBER_CLAIM = "phone_number";

  @Override
  public ApiResponseDTO<TokenResponse> firebaseLogin(FirebaseTokenRequest firebaseLoginRequest) {
    // Verify Firebase ID Token
    FirebaseToken decodedToken =
        firebaseAuthService.verifyIdToken(firebaseLoginRequest.getIdToken());

    String firebaseUid = decodedToken.getUid();
    String phoneNumber = (String) decodedToken.getClaims().get(PHONE_NUMBER_CLAIM);

    log.info("Firebase login attempt - UID: {}, Phone: {}", firebaseUid, phoneNumber);

    // Find existing user
    User user = findExistingUser(firebaseUid, phoneNumber);

    if (user == null) {
      throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    // Update Firebase UID if user was found by phone but doesn't have UID yet
    if (StringUtils.isEmpty(user.getFirebaseUid())) {
      user.setFirebaseUid(firebaseUid);
      user = userRepository.save(user);
    }

    // Generate JWT tokens
    String refreshTokenJti = java.util.UUID.randomUUID().toString();
    String accessToken = jwtService.generateToken(user, refreshTokenJti);
    String refreshToken = jwtService.generateRefreshToken(user, refreshTokenJti);

    TokenResponse loginResponse = createTokenResponse(accessToken, refreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Login successfully", loginResponse);
  }

  @Override
  public ApiResponseDTO<TokenResponse> register(RegisterRequest registerRequest) {
    // Verify Firebase ID Token
    FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(registerRequest.getIdToken());

    String firebaseUid = decodedToken.getUid();
    String phoneNumber = (String) decodedToken.getClaims().get(PHONE_NUMBER_CLAIM);

    log.info("Firebase register attempt - UID: {}, Phone: {}", firebaseUid, phoneNumber);

    // Check if account already exists
    if (checkAccountExists(firebaseUid, phoneNumber)) {
      throw new AppException(ErrorCode.USER_EXISTED);
    }

    // Create new user
    User newUser = createUserFromRegisterRequest(firebaseUid, phoneNumber, registerRequest);

    // Publish user created event to Kafka
    userEventPublisherHelper.publishUserCreatedEvent(newUser);

    // Generate JWT tokens
    String refreshTokenJti = java.util.UUID.randomUUID().toString();
    String accessToken = jwtService.generateToken(newUser, refreshTokenJti);
    String refreshToken = jwtService.generateRefreshToken(newUser, refreshTokenJti);

    TokenResponse tokenResponse = createTokenResponse(accessToken, refreshToken);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User registered successfully", tokenResponse);
  }

  private boolean checkAccountExists(String firebaseUid, String phoneNumber) {
    // Check by Firebase UID or phone number
    if (firebaseUid != null
        && !firebaseUid.isEmpty()
        && userRepository.findByFirebaseUid(firebaseUid).isPresent()) {
      return true;
    }

    return phoneNumber != null
        && !phoneNumber.isEmpty()
        && userRepository.findByPhoneNumber(phoneNumber).isPresent();
  }

  private User findExistingUser(String firebaseUid, String phoneNumber) {
    // Try to find user by Firebase UID first
    if (firebaseUid != null && !firebaseUid.isEmpty()) {
      User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
      if (user != null) {
        log.info("Found existing user by Firebase UID: {}", firebaseUid);
        return user;
      }
    }

    // Try to find user by phone number
    if (phoneNumber != null && !phoneNumber.isEmpty()) {
      User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
      if (user != null) {
        log.info("Found existing user by phone number: {}", phoneNumber);
        return user;
      }
    }

    return null;
  }

  private User createUserFromRegisterRequest(
      String firebaseUid, String phoneNumber, RegisterRequest registerRequest) {
    // Use provided username or generate one
    String username = registerRequest.getUsername();
    if (username == null || username.trim().isEmpty()) {
      username = generateUniqueUsername(firebaseUid);
    } else {
      // Validate username is unique
      if (userRepository.existsByUsername(username)) {
        throw new AppException(ErrorCode.USERNAME_EXISTED);
      }
    }

    // Use provided full name or default to username
    String fullName = registerRequest.getFullName();
    if (fullName == null || fullName.trim().isEmpty()) {
      fullName = username;
    }

    String defaultPhone =
        phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "+00000000000"; // Placeholder

    // Create user with Firebase authentication
    User newUser =
        User.builder()
            .firebaseUid(firebaseUid)
            .username(username)
            .phoneNumber(defaultPhone)
            .role(Role.USER.name())
            .fullName(fullName)
            .build();

    log.info("Creating new user with username: {}", username);
    return userRepository.save(newUser);
  }

  private String generateUniqueUsername(String firebaseUid) {
    // Generate base username from firebase UID
    String baseUsername = "user_" + firebaseUid.substring(0, Math.min(8, firebaseUid.length()));

    // Check if username exists, if yes add random suffix
    String username = baseUsername;
    int counter = 1;
    while (userRepository.existsByUsername(username)) {
      username = baseUsername + "_" + counter;
      counter++;
    }

    return username;
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

  @Override
  public ApiResponseDTO<Void> logOut() {
    Long currentUserId = getCurrentUserId();

    String accessToken = getAccessTokenFromHeader();
    String refreshJti = jwtService.extractRefreshJti(accessToken);

    RefreshToken refreshToken = validateRefreshToken(refreshJti, currentUserId);

    refreshTokenRepository.delete(refreshToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Log out successfully", null);
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

  @Override
  public ApiResponseDTO<RefreshTokenReponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
    String refreshToken = refreshTokenRequest.getRefreshToken();

    String username = jwtService.extractUsername(refreshToken);
    User user = findUserByUsername(username);

    String refreshTokenJti = jwtService.extractJti(refreshToken);
    String newAccessToken = jwtService.generateToken(user, refreshTokenJti);

    RefreshTokenReponse response = createRefreshTokenResponse(newAccessToken);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Token refreshed successfully", response);
  }

  private User findUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private RefreshTokenReponse createRefreshTokenResponse(String accessToken) {
    return RefreshTokenReponse.builder()
        .accessToken(accessToken)
        .expiresIn(jwtProperties.getExpiration())
        .sessionState(java.util.UUID.randomUUID().toString())
        .build();
  }

  @Override
  public ApiResponseDTO<Void> saveDeviceToken(SaveDeviceTokenRequest requestDto) {
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
  public ApiResponseDTO<SaveDeviceTokenResponse> getDeviceToken() {
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
}
