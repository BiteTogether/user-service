package com.bitetogether.user.util;

import com.bitetogether.user.dto.event.UserCreatedEvent;
import com.bitetogether.user.dto.event.UserUpdatedEvent;
import com.bitetogether.user.model.User;
import com.bitetogether.user.service.EventPublisherService;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserEventPublisherHelper {
  EventPublisherService eventPublisherService;

  public void publishUserCreatedEvent(User user) {
    UserCreatedEvent event =
        UserCreatedEvent.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .avatar(user.getAvatar())
            .eventTimestamp(LocalDateTime.now())
            .version(0L)
            .build();

    eventPublisherService.publishUserCreatedEvent(event);
  }

  public void publishUserUpdatedEvent(User user, Long version) {
    UserUpdatedEvent event =
        UserUpdatedEvent.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .avatar(user.getAvatar())
            .eventTimestamp(LocalDateTime.now())
            .version(version)
            .build();

    eventPublisherService.publishUserUpdatedEvent(event);
  }
}
