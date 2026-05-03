package com.bitetogether.user.integration;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitetogether.user.dto.auth.request.FirebaseTokenRequest;
import com.bitetogether.user.dto.auth.request.RefreshTokenRequest;
import com.bitetogether.user.dto.auth.request.RegisterRequest;
import com.bitetogether.user.model.User;
import com.bitetogether.user.service.JwtService;
import com.google.firebase.auth.FirebaseToken;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

  private static final String BASE_URL = "/api/v1/auth";

  @Autowired private JwtService jwtService;

  private FirebaseToken mockFirebaseToken(String uid, String phone) {
    FirebaseToken token = mock(FirebaseToken.class);
    when(token.getUid()).thenReturn(uid);
    Map<String, Object> claims = new HashMap<>();
    claims.put("phone_number", phone);
    when(token.getClaims()).thenReturn(claims);
    return token;
  }

  @Test
  void login_ExistingUser_ReturnsTokens() throws Exception {
    createTestUser("login_user1", "fb-login-1", "+84123456789");

    FirebaseToken firebaseToken = mockFirebaseToken("fb-login-1", "+84123456789");
    when(firebaseAuthService.verifyIdToken(anyString())).thenReturn(firebaseToken);

    FirebaseTokenRequest request = new FirebaseTokenRequest();
    request.setIdToken("valid-firebase-token");

    mockMvc
        .perform(
            post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.access_token", notNullValue()))
        .andExpect(jsonPath("$.data.refresh_token", notNullValue()));
  }

  @Test
  void login_NonExistingUser_ReturnsError() throws Exception {
    FirebaseToken firebaseToken = mockFirebaseToken("fb-nonexist", "+84000000000");
    when(firebaseAuthService.verifyIdToken(anyString())).thenReturn(firebaseToken);

    FirebaseTokenRequest request = new FirebaseTokenRequest();
    request.setIdToken("valid-firebase-token");

    mockMvc
        .perform(
            post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void login_MissingIdToken_Returns400() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_NewUser_ReturnsTokens() throws Exception {
    FirebaseToken firebaseToken = mockFirebaseToken("fb-register-1", "+84111222333");
    when(firebaseAuthService.verifyIdToken(anyString())).thenReturn(firebaseToken);

    RegisterRequest request = new RegisterRequest();
    request.setIdToken("valid-firebase-token");
    request.setUsername("new_reg_usr");
    request.setFullName("New Registered User");

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.access_token", notNullValue()))
        .andExpect(jsonPath("$.data.refresh_token", notNullValue()));
  }

  @Test
  void register_ExistingUser_ReturnsError() throws Exception {
    createTestUser("exist_usr1", "fb-exist-1", "+84444555666");

    FirebaseToken firebaseToken = mockFirebaseToken("fb-exist-1", "+84444555666");
    when(firebaseAuthService.verifyIdToken(anyString())).thenReturn(firebaseToken);

    RegisterRequest request = new RegisterRequest();
    request.setIdToken("valid-firebase-token");
    request.setUsername("another_usr");
    request.setFullName("Another User");

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void register_WithoutUsername_GeneratesOne() throws Exception {
    FirebaseToken firebaseToken = mockFirebaseToken("fb-noreg-1", "+84777888999");
    when(firebaseAuthService.verifyIdToken(anyString())).thenReturn(firebaseToken);

    RegisterRequest request = new RegisterRequest();
    request.setIdToken("valid-firebase-token");

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.access_token", notNullValue()));
  }

  @Test
  void register_MissingIdToken_Returns400() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logout_Authenticated_Success() throws Exception {
    User user = createTestUser("logout_u1", "fb-logout-1", "+84222333444");

    String refreshJti = UUID.randomUUID().toString();
    String accessToken = jwtService.generateToken(user, refreshJti);
    jwtService.generateRefreshToken(user, refreshJti);

    mockMvc
        .perform(delete(BASE_URL + "/logout").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk());
  }

  @Test
  void logout_Unauthenticated_ReturnsError() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/logout")).andExpect(status().is5xxServerError());
  }

  @Test
  void refreshToken_Valid_ReturnsNewAccessToken() throws Exception {
    User user = createTestUser("refresh_u", "fb-refresh-1", "+84555666777");

    String refreshJti = UUID.randomUUID().toString();
    String refreshToken = jwtService.generateRefreshToken(user, refreshJti);

    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshToken);

    // Access to /tokens/refresh is under /api/v1/auth/** which is permitted
    mockMvc
        .perform(
            post(BASE_URL + "/tokens/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.access_token", notNullValue()));
  }

  @Test
  void refreshToken_MissingToken_Returns400() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL + "/tokens/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
