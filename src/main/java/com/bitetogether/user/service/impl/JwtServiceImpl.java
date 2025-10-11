package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.Constants.CLAIM_EMAIL;
import static com.bitetogether.common.util.Constants.CLAIM_JTI;
import static com.bitetogether.common.util.Constants.CLAIM_REFRESH_JTI;
import static com.bitetogether.common.util.Constants.CLAIM_ROLE;
import static com.bitetogether.common.util.Constants.CLAIM_USER_ID;
import static com.bitetogether.common.util.Constants.CLAIM_USER_NAME;

import com.bitetogether.common.configuration.security.JwtProperties;
import com.bitetogether.user.model.RefreshToken;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
  private final JwtProperties jwtProperties;
  private final RefreshTokenRepository refreshTokenRepository;

  @Override
  public String generateToken(User user, String refreshTokenJti) {
    Map<String, Object> claims = new HashMap<>();
    String jti = UUID.randomUUID().toString();

    claims.put(CLAIM_USER_ID, user.getId());
    claims.put(CLAIM_EMAIL, user.getEmail());
    claims.put(CLAIM_USER_NAME, user.getUsername());
    claims.put(CLAIM_ROLE, user.getRole());
    claims.put(CLAIM_REFRESH_JTI, refreshTokenJti);

    LocalDateTime issuedAt = LocalDateTime.now();
    LocalDateTime expiresAt = issuedAt.plusMinutes(jwtProperties.getExpiration());

    return createToken(claims, user.getEmail(), jti, issuedAt, expiresAt);
  }

  @Override
  public String generateRefreshToken(User user, String refreshTokenJti) {
    Map<String, Object> claims = new HashMap<>();

    claims.put(CLAIM_JTI, refreshTokenJti);
    claims.put(CLAIM_USER_ID, user.getId());
    claims.put(CLAIM_EMAIL, user.getEmail());

    LocalDateTime issuedAt = LocalDateTime.now();
    LocalDateTime expiresAt = issuedAt.plusMinutes(jwtProperties.getRefreshExpiration());

    refreshTokenRepository.save(new RefreshToken(refreshTokenJti, user, issuedAt, expiresAt, null));

    return createToken(claims, user.getEmail(), refreshTokenJti, issuedAt, expiresAt);
  }

  private String createToken(
      Map<String, Object> claims,
      String subject,
      String jti,
      LocalDateTime issuedAt,
      LocalDateTime expiresAt) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setId(jti)
        .setIssuedAt(toDate(issuedAt))
        .setExpiration(toDate(expiresAt))
        .signWith(getSignKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private SecretKey getSignKey() {
    byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
  }

  @Override
  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  @Override
  public String extractJti(String refreshToken) {
    return extractClaim(refreshToken, Claims::getId);
  }

  @Override
  public String extractRefreshJti(String accessToken) {
    return extractClaim(accessToken, claims -> claims.get(CLAIM_REFRESH_JTI, String.class));
  }

  @Override
  public boolean isTokenValid(String token) {
    try {
      return !isTokenExpired(token);
    } catch (Exception e) {
      log.error("Invalid token: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  @Override
  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }
}
