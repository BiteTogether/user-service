package com.bitetogether.user.integration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class IntegrationTestConfig {

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return Mockito.mock(KafkaTemplate.class);
  }
}
