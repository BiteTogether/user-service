package com.bitetogether.user.configuration.refreshtoken;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private RefreshTokenCleanupJob cleanupJob;

  @ParameterizedTest
  @ValueSource(ints = {0, 5, 1000, -1})
  void cleanExpiredTokens_WithVariousDeletedCounts_CompletesSuccessfully(int deletedCount) {
    when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
        .thenReturn(deletedCount);

    cleanupJob.cleanExpiredTokens();

    verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }

  @Test
  void cleanExpiredTokens_WhenExceptionThrown_ThrowsAppException() {
    when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(AppException.class, () -> cleanupJob.cleanExpiredTokens());

    verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }
}
