package com.bitetogether.user.configuration.openfeign;

import static com.bitetogether.user.util.AuthUtils.getAccessTokenFromHeader;
import static com.bitetogether.user.util.AuthUtils.getCurrentUserId;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignClientConfig {
  @Bean
  public RequestInterceptor requestInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate template) {
        String token = getAccessTokenFromHeader();
        if (token != null) {
          template.header("Authorization", "Bearer " + token);
        }

        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
          template.header("X-User-Id", String.valueOf(currentUserId));
        }

        log.debug(
            "Feign header propagation - hasAuth: {}, hasUserId: {}",
            token != null,
            currentUserId != null);
      }
    };
  }
}
