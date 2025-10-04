package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.Constants.CLAIM_EMAIL;
import static com.bitetogether.common.util.Constants.CLAIM_ROLE;
import static com.bitetogether.common.util.Constants.CLAIM_USER_ID;
import static com.bitetogether.common.util.Constants.CLAIM_USER_NAME;

import com.bitetogether.common.configuration.security.JwtProperties;
import com.bitetogether.user.model.User;
import com.bitetogether.user.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

  public String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_USER_ID, user.getId());
    claims.put(CLAIM_EMAIL, user.getEmail());
    claims.put(CLAIM_USER_NAME, user.getUsername());
    claims.put(CLAIM_ROLE, user.getRole());

    return createToken(claims, user.getEmail());
  }

  public String generateRefreshToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_USER_ID, user.getId());
    claims.put(CLAIM_EMAIL, user.getEmail());

    return createRefreshToken(claims, user.getEmail());
  }

  private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
        .signWith(getSignKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private String createRefreshToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiration()))
        .signWith(getSignKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private SecretKey getSignKey() {
    byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
  }

  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public boolean isTokenValid(String token) {
    try {
      return !isTokenExpired(token);
    } catch (Exception e) {
      log.error("Invalid token: {}", e.getMessage());
      return false;
    }
  }

  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }
}
