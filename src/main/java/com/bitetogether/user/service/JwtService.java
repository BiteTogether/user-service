package com.bitetogether.user.service;

import com.bitetogether.user.model.User;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.function.Function;

public interface JwtService {
  String generateToken(User user);

  String generateRefreshToken(User user);

  String extractUsername(String token);

  Date extractExpiration(String token);

  <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

  Boolean validateToken(String token, String username);
}
