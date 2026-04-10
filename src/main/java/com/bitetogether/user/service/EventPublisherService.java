package com.bitetogether.user.service;

import com.bitetogether.user.dto.event.CreateConversationEvent;
import com.bitetogether.user.dto.event.UserCreatedEvent;
import com.bitetogether.user.dto.event.UserDeletedEvent;
import com.bitetogether.user.dto.event.UserUpdatedEvent;

public interface EventPublisherService {
  void publishUserCreatedEvent(UserCreatedEvent event);

  void publishUserUpdatedEvent(UserUpdatedEvent event);

  void publishUserDeletedEvent(UserDeletedEvent event);

  void publishCreateConversationEvent(CreateConversationEvent event);
}
