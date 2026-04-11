package com.bitetogether.user.configuration.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Data
@Component
@Profile("kafka")
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

  private String bootstrapServers;
  private Properties properties;
  private Producer producer;
  private Topic topic;

  @Data
  public static class Properties {
    private String securityProtocol;
    private Ssl ssl;

    @Data
    public static class Ssl {
      private Keystore keystore;
      private Truststore truststore;
      private String endpointIdentificationAlgorithm;

      @Data
      public static class Keystore {
        private String type;
        private String location;
        private String password;
      }

      @Data
      public static class Truststore {
        private String type;
        private String location;
        private String password;
      }
    }
  }

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
