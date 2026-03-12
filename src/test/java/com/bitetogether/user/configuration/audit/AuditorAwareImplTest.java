package com.bitetogether.user.configuration.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.bitetogether.user.util.AuthUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
  "java:S2699",
  "java:S6073"
}) // Sonar: assertions present, unboxing warnings are false positives
class AuditorAwareImplTest {

  @InjectMocks private AuditorAwareImpl auditorAware;

  @Test
  void getCurrentAuditor_WithValidUserId_ReturnsUserAuditor() {
    Long userId = 123L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      Optional<String> auditor = auditorAware.getCurrentAuditor();

      assertTrue(auditor.isPresent());
      assertEquals("USER_123", auditor.get());
    }
  }

  @Test
  void getCurrentAuditor_WithNullUserId_ReturnsSystemAuditor() {
    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(null);

      Optional<String> auditor = auditorAware.getCurrentAuditor();

      assertTrue(auditor.isPresent());
      assertEquals("SYSTEM", auditor.get());
    }
  }

  @Test
  void getCurrentAuditor_WhenExceptionThrown_ReturnsSystemAuditor() {
    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock
          .when(AuthUtils::getCurrentUserId)
          .thenThrow(new RuntimeException("Authentication error"));

      Optional<String> auditor = auditorAware.getCurrentAuditor();

      assertTrue(auditor.isPresent());
      assertEquals("SYSTEM", auditor.get());
    }
  }

  @Test
  void getCurrentAuditor_WithZeroUserId_ReturnsUserAuditor() {
    Long userId = 0L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      Optional<String> auditor = auditorAware.getCurrentAuditor();

      assertTrue(auditor.isPresent());
      assertEquals("USER_0", auditor.get());
    }
  }

  @Test
  void getCurrentAuditor_WithLargeUserId_ReturnsUserAuditor() {
    Long userId = 999999999L;

    try (MockedStatic<AuthUtils> authUtilsMock = mockStatic(AuthUtils.class)) {
      authUtilsMock.when(AuthUtils::getCurrentUserId).thenReturn(userId);

      Optional<String> auditor = auditorAware.getCurrentAuditor();

      assertTrue(auditor.isPresent());
      assertEquals("USER_999999999", auditor.get());
    }
  }
}
