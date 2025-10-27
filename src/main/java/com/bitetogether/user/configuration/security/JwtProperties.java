package com.bitetogether.user.configuration.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
  private String secretKey;
  private long expiration;
  private long refreshExpiration;
}
