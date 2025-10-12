package com.bitetogether.user.service;

import com.bitetogether.user.model.User;
import java.util.Date;

public interface JwtService {
  String generateToken(User user, String refreshTokenJti);

  String generateRefreshToken(User user, String refreshTokenJti);

  Date extractExpiration(String token);

  String extractEmail(String token);

  String extractJti(String refreshToken);

  String extractRefreshJti(String accessToken);

  boolean isTokenValid(String token);

  boolean isTokenExpired(String token);
}
