package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.enums.Role;
import com.bitetogether.user.configuration.security.JwtProperties;
import com.bitetogether.user.model.RefreshToken;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

  @Mock private JwtProperties jwtProperties;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private JwtServiceImpl jwtService;

  private User testUser;
  private String secretKey;
  private String refreshTokenJti;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .phoneNumber("test@example.com")
            .role(Role.USER.name())
            .build();

    secretKey = "ThisIsAVerySecureSecretKeyForJWTTokenGenerationAndValidation1234567890";
    refreshTokenJti = UUID.randomUUID().toString();

    lenient().when(jwtProperties.getSecretKey()).thenReturn(secretKey);
    lenient().when(jwtProperties.getExpiration()).thenReturn(60L);
    lenient().when(jwtProperties.getRefreshExpiration()).thenReturn(10080L);
  }

  @Test
  void generateToken_WithValidUser_ReturnsToken() {
    String token = jwtService.generateToken(testUser, refreshTokenJti);

    assertNotNull(token);
    assertEquals(3, token.split("\\.").length);

    Claims claims = extractClaims(token);
    assertEquals(testUser.getId(), claims.get("userId", Long.class));
    assertEquals(testUser.getUsername(), claims.get("username"));
    assertEquals(testUser.getRole(), claims.get("role"));
    assertEquals(testUser.getUsername(), claims.getSubject());
    assertEquals("bitetogether.com", claims.getIssuer());
    assertNotNull(claims.getId());
  }

  @Test
  void generateRefreshToken_WithValidUser_ReturnsTokenAndSavesRefreshToken() {
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String token = jwtService.generateRefreshToken(testUser, refreshTokenJti);

    assertNotNull(token);
    assertEquals(3, token.split("\\.").length);

    Claims claims = extractClaims(token);
    assertEquals(refreshTokenJti, claims.getId());
    assertEquals(refreshTokenJti, claims.get("jti"));
    assertEquals(testUser.getId(), claims.get("userId", Long.class));
    assertEquals(testUser.getUsername(), claims.get("username"));
    assertEquals(testUser.getUsername(), claims.getSubject());
    assertEquals("bitetogether.com", claims.getIssuer());

    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
  }

  @Test
  void extractUsername_WithValidToken_ReturnsUsername() {
    String token = jwtService.generateToken(testUser, refreshTokenJti);

    String username = jwtService.extractUsername(token);

    assertEquals(testUser.getUsername(), username);
  }

  @Test
  void extractJti_WithValidRefreshToken_ReturnsJti() {
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String token = jwtService.generateRefreshToken(testUser, refreshTokenJti);

    String jti = jwtService.extractJti(token);

    assertEquals(refreshTokenJti, jti);
  }

  @Test
  void extractRefreshJti_WithValidAccessToken_ReturnsRefreshJti() {
    String token = jwtService.generateToken(testUser, refreshTokenJti);

    String extractedRefreshJti = jwtService.extractRefreshJti(token);

    assertEquals(refreshTokenJti, extractedRefreshJti);
  }

  @Test
  void extractExpiration_WithValidToken_ReturnsExpirationDate() {
    Date beforeGeneration = new Date();
    String token = jwtService.generateToken(testUser, refreshTokenJti);

    Date expiration = jwtService.extractExpiration(token);

    assertNotNull(expiration);
    assertTrue(expiration.after(beforeGeneration));
  }

  @Test
  void generateToken_WithDifferentUser_GeneratesDifferentTokens() {
    String token1 = jwtService.generateToken(testUser, refreshTokenJti);

    User anotherUser =
        User.builder()
            .id(2L)
            .username("anotheruser")
            .phoneNumber("another@example.com")
            .role(Role.USER.name())
            .build();

    String token2 = jwtService.generateToken(anotherUser, refreshTokenJti);

    assertNotNull(token1);
    assertNotNull(token2);
    assertNotEquals(token1, token2);
  }

  @Test
  void generateRefreshToken_SavesTokenWithCorrectData() {
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(
            invocation -> {
              RefreshToken savedToken = invocation.getArgument(0);
              assertEquals(refreshTokenJti, savedToken.getJti());
              assertEquals(testUser, savedToken.getUser());
              assertNotNull(savedToken.getIssuedAt());
              assertNotNull(savedToken.getExpiresAt());
              assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(10000)));
              return savedToken;
            });

    jwtService.generateRefreshToken(testUser, refreshTokenJti);

    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
  }

  @Test
  void generateToken_IncludesCorrectExpiration() {
    long expirationMinutes = 30L;
    when(jwtProperties.getExpiration()).thenReturn(expirationMinutes);

    String token = jwtService.generateToken(testUser, refreshTokenJti);

    Date expiration = jwtService.extractExpiration(token);
    long expirationTime = expiration.getTime();
    long currentTime = System.currentTimeMillis();
    long diffInMinutes = (expirationTime - currentTime) / (1000 * 60);

    assertTrue(diffInMinutes >= expirationMinutes - 1 && diffInMinutes <= expirationMinutes + 1);
  }

  private Claims extractClaims(String token) {
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
  }
}
