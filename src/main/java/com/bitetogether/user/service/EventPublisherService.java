package com.bitetogether.user.service;

import com.bitetogether.user.dto.event.UserUpdatedEvent;
import com.bitetogether.user.dto.event.UserCreatedEvent;
import com.bitetogether.user.dto.event.CreateConversationEvent;

public interface EventPublisherService {
  void publishUserCreatedEvent(UserCreatedEvent event);

  void publishUserUpdatedEvent(UserUpdatedEvent event);

  void publishCreateConversationEvent(CreateConversationEvent event);
}






