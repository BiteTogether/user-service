package com.bitetogether.user.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.user.configuration.kafka.KafkaProperties;
import com.bitetogether.user.dto.event.CreateConversationEvent;
import com.bitetogether.user.dto.event.UserCreatedEvent;
import com.bitetogether.user.dto.event.UserDeletedEvent;
import com.bitetogether.user.dto.event.UserUpdatedEvent;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {
  @Mock private KafkaTemplate<String, Object> kafkaTemplate;
  @Mock private KafkaProperties kafkaProperties;
  @Mock private KafkaProperties.Topic topic;
  @InjectMocks private EventPublisherServiceImpl eventPublisherService;

  @BeforeEach
  void setUp() {
    when(kafkaProperties.getTopic()).thenReturn(topic);
  }

  @Test
  void publishUserCreatedEvent_SendsToKafka() {
    when(topic.getUserEvents()).thenReturn("user-events");
    when(kafkaTemplate.send(anyString(), anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    UserCreatedEvent event =
        UserCreatedEvent.builder()
            .userId(1L)
            .username("test")
            .eventTimestamp(LocalDateTime.now())
            .build();
    eventPublisherService.publishUserCreatedEvent(event);
    verify(kafkaTemplate).send(eq("user-events"), eq("1"), eq(event));
  }

  @Test
  void publishUserUpdatedEvent_SendsToKafka() {
    when(topic.getUserEvents()).thenReturn("user-events");
    when(kafkaTemplate.send(anyString(), anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    UserUpdatedEvent event =
        UserUpdatedEvent.builder()
            .userId(2L)
            .username("test")
            .eventTimestamp(LocalDateTime.now())
            .build();
    eventPublisherService.publishUserUpdatedEvent(event);
    verify(kafkaTemplate).send(eq("user-events"), eq("2"), eq(event));
  }

  @Test
  void publishUserDeletedEvent_SendsToKafka() {
    when(topic.getUserEvents()).thenReturn("user-events");
    when(kafkaTemplate.send(anyString(), anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    UserDeletedEvent event =
        UserDeletedEvent.builder().userId(3L).eventTimestamp(LocalDateTime.now()).build();
    eventPublisherService.publishUserDeletedEvent(event);
    verify(kafkaTemplate).send(eq("user-events"), eq("3"), eq(event));
  }

  @Test
  void publishCreateConversationEvent_SendsToKafka() {
    when(topic.getConversationEvents()).thenReturn("conv-events");
    when(kafkaTemplate.send(anyString(), anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    CreateConversationEvent event =
        CreateConversationEvent.builder()
            .user1Id(1L)
            .user2Id(2L)
            .eventTimestamp(LocalDateTime.now())
            .build();
    eventPublisherService.publishCreateConversationEvent(event);
    verify(kafkaTemplate).send(eq("conv-events"), eq("1-2"), eq(event));
  }

  @Test
  void publishUserCreatedEvent_WhenFails_HandlesGracefully() {
    when(topic.getUserEvents()).thenReturn("user-events");
    CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Kafka error"));
    when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
    UserCreatedEvent event =
        UserCreatedEvent.builder()
            .userId(1L)
            .username("test")
            .eventTimestamp(LocalDateTime.now())
            .build();
    eventPublisherService.publishUserCreatedEvent(event);
    verify(kafkaTemplate).send(eq("user-events"), eq("1"), eq(event));
  }
}
