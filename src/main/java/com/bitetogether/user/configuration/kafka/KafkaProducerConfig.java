package com.bitetogether.user.configuration.kafka;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Slf4j
@Configuration
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaProducerConfig {
  private final KafkaProperties kafkaProperties;
  private final ResourceLoader resourceLoader;

  @Bean
  public ProducerFactory<String, Object> producerFactory() throws IOException {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
    props.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        kafkaProperties.getProducer().getKeySerializer());
    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        kafkaProperties.getProducer().getValueSerializer());
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    // Handle SSL configuration if present
    var kafkaProps = kafkaProperties.getProperties();
    if (kafkaProps != null && kafkaProps.getSsl() != null) {
      var ssl = kafkaProps.getSsl();

      // Set security protocol
      if (kafkaProps.getSecurityProtocol() != null) {
        props.put("security.protocol", kafkaProps.getSecurityProtocol());
        log.info("Kafka security protocol: {}", kafkaProps.getSecurityProtocol());
      }

      // Configure keystore - Convert classpath: to absolute path
      if (ssl.getKeystore() != null) {
        Resource keystoreResource = resourceLoader.getResource(ssl.getKeystore().getLocation());
        String keystorePath = keystoreResource.getFile().getAbsolutePath();

        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, ssl.getKeystore().getType());
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ssl.getKeystore().getPassword());

        log.info("✅ Loaded keystore from: {}", keystorePath);
      }

      // Configure truststore - Convert classpath: to absolute path
      if (ssl.getTruststore() != null) {
        Resource truststoreResource = resourceLoader.getResource(ssl.getTruststore().getLocation());
        String truststorePath = truststoreResource.getFile().getAbsolutePath();

        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, ssl.getTruststore().getType());
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.getTruststore().getPassword());

        log.info("✅ Loaded truststore from: {}", truststorePath);
      }

      // Set endpoint identification algorithm
      if (ssl.getEndpointIdentificationAlgorithm() != null) {
        props.put(
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
            ssl.getEndpointIdentificationAlgorithm());
      }
    }

    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() throws IOException {
    return new KafkaTemplate<>(producerFactory());
  }
}
