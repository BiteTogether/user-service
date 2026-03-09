package com.bitetogether.user.configuration.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

  private String bootstrapServers;
  private Producer producer;
  private Topic topic;

  @Data
  public static class Producer {
    private String keySerializer;
    private String valueSerializer;
  }

  @Data
  public static class Topic {
    private String userEvents;
    private String conversationEvents;
  }
}
