package com.bitetogether.user.service.impl;

import com.bitetogether.user.configuration.kafka.KafkaProperties;
import com.bitetogether.user.dto.event.CreateConversationEvent;
import com.bitetogether.user.dto.event.UserCreatedEvent;
import com.bitetogether.user.dto.event.UserUpdatedEvent;
import com.bitetogether.user.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  @Override
  public void publishUserCreatedEvent(UserCreatedEvent event) {
    String topic = kafkaProperties.getTopic().getUserEvents();
    kafkaTemplate
        .send(topic, event.getUserId().toString(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex == null) {
                logSuccess("UserCreatedEvent", result);
              } else {
                logError("UserCreatedEvent", topic, ex);
              }
            });
  }

  @Override
  public void publishUserUpdatedEvent(UserUpdatedEvent event) {
    String topic = kafkaProperties.getTopic().getUserEvents();
    kafkaTemplate
        .send(topic, event.getUserId().toString(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex == null) {
                logSuccess("UserUpdatedEvent", result);
              } else {
                logError("UserUpdatedEvent", topic, ex);
              }
            });
  }

  @Override
  public void publishCreateConversationEvent(CreateConversationEvent event) {
    String topic = kafkaProperties.getTopic().getConversationEvents();
    String key = event.getUser1Id() + "-" + event.getUser2Id();
    kafkaTemplate
        .send(topic, key, event)
        .whenComplete(
            (result, ex) -> {
              if (ex == null) {
                logSuccess("CreateConversationEvent", result);
              } else {
                logError("CreateConversationEvent", topic, ex);
              }
            });
  }

  private void logSuccess(String eventType, SendResult<String, Object> result) {
    log.info(
        "Successfully published {} to topic: {}, partition: {}, offset: {}",
        eventType,
        result.getRecordMetadata().topic(),
        result.getRecordMetadata().partition(),
        result.getRecordMetadata().offset());
  }

  private void logError(String eventType, String topic, Throwable ex) {
    log.error("Failed to publish {} to topic: {}. Error: {}", eventType, topic, ex.getMessage());
  }
}
