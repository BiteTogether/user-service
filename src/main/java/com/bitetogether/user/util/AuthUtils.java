package com.bitetogether.user.util;

import static com.bitetogether.common.util.Constants.CLAIM_EMAIL;
import static com.bitetogether.common.util.Constants.CLAIM_USER_ID;
import static com.bitetogether.common.util.Constants.CLAIM_USER_NAME;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthUtils {

  private static Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private static Jwt getCurrentJwt() {
    Authentication authentication = getAuthentication();
    return (authentication != null && authentication.getPrincipal() instanceof Jwt jwt)
        ? jwt
        : null;
  }

  public static boolean hasRole(String role) {
    Authentication authentication = getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals(role));
  }

  public static Long getCurrentUserId() {
    Jwt jwt = getCurrentJwt();
    if (jwt != null) {
      Object userIdObj = jwt.getClaim(CLAIM_USER_ID);
      if (userIdObj instanceof Integer integer) {
        return integer.longValue();
      } else if (userIdObj instanceof Long longValue) {
        return longValue;
      }
    }
    return null;
  }

  public static String getCurrentUserName() {
    Jwt jwt = getCurrentJwt();
    if (jwt != null) {
      Object userUsernameObj = jwt.getClaim(CLAIM_USER_NAME);
      return userUsernameObj instanceof String username ? username : null;
    }
    return null;
  }

  public static String getCurrentUserEmail() {
    Jwt jwt = getCurrentJwt();
    if (jwt != null) {
      Object userEmailObj = jwt.getClaim(CLAIM_EMAIL);
      return userEmailObj instanceof String email ? email : null;
    }
    return null;
  }

  public static String getAccessTokenFromHeader() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      HttpServletRequest request = attrs.getRequest();
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
      }
    }
    return null;
  }
}
