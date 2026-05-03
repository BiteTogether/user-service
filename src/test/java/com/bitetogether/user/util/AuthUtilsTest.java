package com.bitetogether.user.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthUtilsTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUserId_WithIntegerClaim_ReturnsLong() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", 42);
    claims.put("sub", "testuser");
    Jwt jwt =
        new Jwt(
            "token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);
    var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertEquals(42L, AuthUtils.getCurrentUserId());
  }

  @Test
  void getCurrentUserId_WithLongClaim_ReturnsLong() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", 42L);
    claims.put("sub", "testuser");
    Jwt jwt =
        new Jwt(
            "token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);
    var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertEquals(42L, AuthUtils.getCurrentUserId());
  }

  @Test
  void getCurrentUserId_WithNoAuthentication_ReturnsNull() {
    SecurityContextHolder.clearContext();
    assertNull(AuthUtils.getCurrentUserId());
  }

  @Test
  void getCurrentUserId_WithNonJwtPrincipal_ReturnsNull() {
    var auth = new UsernamePasswordAuthenticationToken("user", "pass", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
    assertNull(AuthUtils.getCurrentUserId());
  }

  @Test
  void getCurrentUserId_WithNullUserIdClaim_ReturnsNull() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "testuser");
    Jwt jwt =
        new Jwt(
            "token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);
    var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
    assertNull(AuthUtils.getCurrentUserId());
  }

  @Test
  void getCurrentUserName_WithValidClaim_ReturnsUsername() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("username", "testuser");
    claims.put("sub", "testuser");
    Jwt jwt =
        new Jwt(
            "token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);
    var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
    assertEquals("testuser", AuthUtils.getCurrentUserName());
  }

  @Test
  void getCurrentUserName_WithNoAuthentication_ReturnsNull() {
    SecurityContextHolder.clearContext();
    assertNull(AuthUtils.getCurrentUserName());
  }

  @Test
  void hasRole_WithMatchingRole_ReturnsTrue() {
    Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));
    var auth = new UsernamePasswordAuthenticationToken("user", "pass", authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
    assertTrue(AuthUtils.hasRole("USER"));
  }

  @Test
  void hasRole_WithNonMatchingRole_ReturnsFalse() {
    Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));
    var auth = new UsernamePasswordAuthenticationToken("user", "pass", authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
    assertFalse(AuthUtils.hasRole("ADMIN"));
  }

  @Test
  void hasRole_WithNoAuthentication_ReturnsFalse() {
    SecurityContextHolder.clearContext();
    assertFalse(AuthUtils.hasRole("USER"));
  }

  @Test
  void getAccessTokenFromHeader_WithNoRequestContext_ReturnsNull() {
    assertNull(AuthUtils.getAccessTokenFromHeader());
  }
}
