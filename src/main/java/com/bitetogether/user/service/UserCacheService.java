package com.bitetogether.user.service;

import com.bitetogether.user.dto.user.response.UserStateResponse;
import com.bitetogether.user.enums.UserState;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserCacheService {

  void cacheUserState(
      Long userId, UserState state, LocalDateTime lastSeen, boolean pushNotificationsEnabled);

  Optional<UserStateResponse> getCachedUserState(Long userId);

  void evictUserState(Long userId);
}
