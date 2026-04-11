package com.bitetogether.user.configuration.redis;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
public class CacheProperties {
  private UserCacheConfig user = new UserCacheConfig();

  @Getter
  @Setter
  public static class UserCacheConfig {
    private Duration ttl = Duration.ofHours(1);
  }
}
