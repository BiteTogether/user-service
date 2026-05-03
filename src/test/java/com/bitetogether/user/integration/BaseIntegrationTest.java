package com.bitetogether.user.integration;

import com.bitetogether.user.client.ConversationClient;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.FriendRequestRepository;
import com.bitetogether.user.repository.RefreshTokenRepository;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.EventPublisherService;
import com.bitetogether.user.service.FirebaseAuthService;
import com.bitetogether.user.service.impl.FirebaseStorageServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
@Transactional
public abstract class BaseIntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected UserRepository userRepository;
  @Autowired protected FriendRequestRepository friendRequestRepository;
  @Autowired protected RefreshTokenRepository refreshTokenRepository;

  @MockitoBean protected FirebaseApp firebaseApp;
  @MockitoBean protected Storage storage;
  @MockitoBean protected FirebaseAuthService firebaseAuthService;
  @MockitoBean protected FirebaseStorageServiceImpl firebaseStorageService;
  @MockitoBean protected ConversationClient conversationClient;
  @MockitoBean protected RedisConnectionFactory redisConnectionFactory;
  @MockitoBean protected EventPublisherService eventPublisherService;

  @Value("${app.jwt.secret-key}")
  private String secretKey;

  protected User createTestUser(String username, String firebaseUid, String phone) {
    User user =
        User.builder()
            .username(username)
            .firebaseUid(firebaseUid)
            .fullName("Test " + username)
            .phoneNumber(phone)
            .role("USER")
            .build();
    return userRepository.save(user);
  }

  protected String generateJwtToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    String refreshJti = UUID.randomUUID().toString();
    claims.put("userId", user.getId());
    claims.put("userName", user.getUsername());
    claims.put("role", user.getRole());
    claims.put("refreshJti", refreshJti);

    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(user.getUsername())
        .setId(UUID.randomUUID().toString())
        .setIssuer("bitetogether.com")
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 900000))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }
}
