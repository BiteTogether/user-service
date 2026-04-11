package com.bitetogether.user.service.impl;

import com.bitetogether.user.configuration.redis.CacheProperties;
import com.bitetogether.user.dto.user.response.UserStateResponse;
import com.bitetogether.user.enums.UserState;
import com.bitetogether.user.service.UserCacheService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCacheServiceImpl implements UserCacheService {
  private final StringRedisTemplate stringRedisTemplate;
  private final CacheProperties cacheProperties;

  private static final String USER_STATE_CACHE_KEY_PREFIX = "user:state:";
  private static final String CACHE_DELIMITER = ":";

  @Override
  public void cacheUserState(
      Long userId, UserState state, LocalDateTime lastSeen, boolean pushNotificationsEnabled) {
    Duration ttl = cacheProperties.getUser().getTtl();

    String key = USER_STATE_CACHE_KEY_PREFIX + userId;
    // Format: state:lastSeen:pushNotificationsEnabled
    String value =
        state.name()
            + CACHE_DELIMITER
            + lastSeen.toString()
            + CACHE_DELIMITER
            + pushNotificationsEnabled;

    stringRedisTemplate.opsForValue().set(key, value, ttl);

    log.debug(
        "Cached user state, lastSeen, and pushNotificationsEnabled for user ID {}: {} at {} (push: {})",
        userId,
        state,
        lastSeen,
        pushNotificationsEnabled);
  }

  @Override
  public Optional<UserStateResponse> getCachedUserState(Long userId) {
    String key = USER_STATE_CACHE_KEY_PREFIX + userId;
    String cachedValue = stringRedisTemplate.opsForValue().get(key);

    if (cachedValue == null) {
      log.debug("User state not found in cache for user ID: {}", userId);
      return Optional.empty();
    }

    String[] parts = cachedValue.split(CACHE_DELIMITER, 3);
    if (parts.length != 3) {
      log.warn("Invalid cache format for user ID {}: {}", userId, cachedValue);
      return Optional.empty();
    }

    try {
      UserStateResponse response =
          UserStateResponse.builder()
              .state(UserState.valueOf(parts[0]))
              .lastSeen(LocalDateTime.parse(parts[1]))
              .pushNotificationsEnabled(Boolean.parseBoolean(parts[2]))
              .build();

      log.debug("Retrieved user state from cache for user ID: {}", userId);
      return Optional.of(response);
    } catch (Exception e) {
      log.error("Error parsing cached user state for user ID {}: {}", userId, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void evictUserState(Long userId) {
    String key = USER_STATE_CACHE_KEY_PREFIX + userId;
    stringRedisTemplate.delete(key);
    log.debug("Evicted user state from cache for user ID: {}", userId);
  }
}
