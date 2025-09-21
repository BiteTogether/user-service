package com.bitetogether.user.configuration.security;

import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

  private String permitPaths;
  private String allowedOrigins;
  private String allowedMethods;
  private String allowedHeaders;

  private boolean allowCredentials;

  public List<String> getPermitPaths() {
    return split(permitPaths);
  }

  public List<String> getAllowedOrigins() {
    return split(allowedOrigins);
  }

  public List<String> getAllowedMethods() {
    return split(allowedMethods);
  }

  public List<String> getAllowedHeaders() {
    return split(allowedHeaders);
  }

  private List<String> split(String raw) {
    return raw == null || raw.isBlank() ? List.of() : Arrays.asList(raw.split("\\s*,\\s*"));
  }
}
