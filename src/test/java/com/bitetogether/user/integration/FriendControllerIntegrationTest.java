package com.bitetogether.user.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitetogether.user.model.User;
import org.junit.jupiter.api.Test;

class FriendControllerIntegrationTest extends BaseIntegrationTest {

  private static final String BASE_URL = "/api/v1/friends";

  private void makeFriends(User user1, User user2) {
    user1.getFriends().add(user2);
    user2.getFriends().add(user1);
    userRepository.save(user1);
    userRepository.save(user2);
  }

  @Test
  void getFriendsList_EmptyList_Success() throws Exception {
    User user = createTestUser("friend_u1", "fb-fr-1", "1111111111");
    String token = generateJwtToken(user);

    mockMvc
        .perform(get(BASE_URL).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void getFriendsList_WithFriends_Success() throws Exception {
    User user1 = createTestUser("friend_u2", "fb-fr-2", "2222222222");
    User user2 = createTestUser("friend_u3", "fb-fr-3", "3333333333");
    makeFriends(user1, user2);
    String token = generateJwtToken(user1);

    mockMvc
        .perform(
            get(BASE_URL)
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void getFriendsList_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void deleteFriend_Success() throws Exception {
    User user1 = createTestUser("delfr_u1", "fb-delfr-1", "4444444444");
    User user2 = createTestUser("delfr_u2", "fb-delfr-2", "5555555555");
    makeFriends(user1, user2);
    String token = generateJwtToken(user1);

    mockMvc
        .perform(delete(BASE_URL + "/" + user2.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void deleteFriend_NotFriend_ReturnsError() throws Exception {
    User user1 = createTestUser("notfr_u1", "fb-notfr-1", "6666666666");
    User user2 = createTestUser("notfr_u2", "fb-notfr-2", "7777777777");
    String token = generateJwtToken(user1);

    mockMvc
        .perform(delete(BASE_URL + "/" + user2.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteFriend_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isUnauthorized());
  }
}
