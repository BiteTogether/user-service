package com.bitetogether.user.configuration.refreshtoken;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

  private final RefreshTokenRepository refreshTokenRepository;

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void cleanExpiredTokens() {
    try {
      LocalDateTime now = LocalDateTime.now();
      int deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(now);

      if (deletedCount > 0) {
        log.info("Successfully deleted {} expired refresh tokens", deletedCount);
      } else {
        log.debug("No expired refresh tokens found for cleanup");
      }
    } catch (Exception e) {
      log.error("Error occurred during refresh token cleanup", e);
      throw new AppException(ErrorCode.REFRESH_TOKEN_ERROR);
    }
  }
}
