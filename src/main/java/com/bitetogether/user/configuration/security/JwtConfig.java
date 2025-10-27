package com.bitetogether.user.configuration.security;

import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {
  private final JwtProperties jwtProperties;

  @Bean
  public JwtDecoder jwtDecoder() {
    String signerKey = jwtProperties.getSecretKey();
    SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix("");
    authoritiesConverter.setAuthoritiesClaimName("role");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          var authorities = authoritiesConverter.convert(jwt);

          System.out.println("=== JWT AUTHENTICATION CONVERTER DEBUG ===");
          System.out.println("JWT Subject: " + jwt.getSubject());
          System.out.println("JWT Claims: " + jwt.getClaims());
          System.out.println("Role claim value: " + jwt.getClaimAsString("role"));
          System.out.println("Extracted authorities: " + authorities);
          System.out.println("Authority prefix: ''");
          System.out.println("Authorities claim name: role");
          System.out.println("=== END JWT AUTHENTICATION CONVERTER DEBUG ===");

          return authorities;
        });

    return converter;
  }
}
