package com.bitetogether.user.configuration.audit;

import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    try {
      Long currentUserId = getCurrentUserId();

      if (currentUserId != null) {
        String auditor = "USER_" + currentUserId;
        return Optional.of(auditor);
      }

      return Optional.of("SYSTEM");

    } catch (Exception e) {
      return Optional.of("SYSTEM");
    }
  }
}
