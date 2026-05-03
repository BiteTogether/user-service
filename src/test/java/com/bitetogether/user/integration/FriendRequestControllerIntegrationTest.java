package com.bitetogether.user.integration;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import org.junit.jupiter.api.Test;

class FriendRequestControllerIntegrationTest extends BaseIntegrationTest {

  private static final String BASE_URL = "/api/v1/friend-requests";

  @Test
  void createFriendRequest_Success() throws Exception {
    User sender = createTestUser("sender_u1", "fb-snd-1", "1111111111");
    User receiver = createTestUser("recv_usr1", "fb-rcv-1", "2222222222");
    String token = generateJwtToken(sender);

    mockMvc
        .perform(post(BASE_URL + "/" + receiver.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", notNullValue()));
  }

  @Test
  void createFriendRequest_ToSelf_ReturnsError() throws Exception {
    User user = createTestUser("self_usr1", "fb-self-1", "3333333333");
    String token = generateJwtToken(user);

    mockMvc
        .perform(post(BASE_URL + "/" + user.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createFriendRequest_Duplicate_ReturnsError() throws Exception {
    User sender = createTestUser("dup_snd_1", "fb-dup-s1", "4444444444");
    User receiver = createTestUser("dup_rcv_1", "fb-dup-r1", "5555555555");
    friendRequestRepository.save(FriendRequest.builder().sender(sender).receiver(receiver).build());
    String token = generateJwtToken(sender);

    mockMvc
        .perform(post(BASE_URL + "/" + receiver.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
  }

  @Test
  void createFriendRequest_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(post(BASE_URL + "/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void acceptFriendRequest_Success() throws Exception {
    User sender = createTestUser("acc_snd_1", "fb-acc-s1", "6666666666");
    User receiver = createTestUser("acc_rcv_1", "fb-acc-r1", "7777777777");
    FriendRequest fr =
        friendRequestRepository.save(
            FriendRequest.builder().sender(sender).receiver(receiver).build());
    String token = generateJwtToken(receiver);

    mockMvc
        .perform(
            post(BASE_URL + "/" + fr.getId() + "/accept")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void acceptFriendRequest_NotReceiver_ReturnsError() throws Exception {
    User sender = createTestUser("nrcv_snd", "fb-nrcv-s", "8888888881");
    User receiver = createTestUser("nrcv_rcv", "fb-nrcv-r", "8888888882");
    User other = createTestUser("nrcv_oth", "fb-nrcv-o", "8888888883");
    FriendRequest fr =
        friendRequestRepository.save(
            FriendRequest.builder().sender(sender).receiver(receiver).build());
    String token = generateJwtToken(other);

    mockMvc
        .perform(
            post(BASE_URL + "/" + fr.getId() + "/accept")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteFriendRequest_Success() throws Exception {
    User sender = createTestUser("del_snd_1", "fb-del-s1", "9999999991");
    User receiver = createTestUser("del_rcv_1", "fb-del-r1", "9999999992");
    FriendRequest fr =
        friendRequestRepository.save(
            FriendRequest.builder().sender(sender).receiver(receiver).build());
    String token = generateJwtToken(sender);

    mockMvc
        .perform(delete(BASE_URL + "/" + fr.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void deleteFriendRequest_NotFound_ReturnsError() throws Exception {
    User user = createTestUser("delnf_u1", "fb-delnf-1", "1010101010");
    String token = generateJwtToken(user);

    mockMvc
        .perform(delete(BASE_URL + "/99999").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteFriendRequest_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void getSentFriendRequests_Success() throws Exception {
    User sender = createTestUser("sent_u1", "fb-sent-1", "1212121212");
    User receiver = createTestUser("sent_u2", "fb-sent-2", "1313131313");
    friendRequestRepository.save(FriendRequest.builder().sender(sender).receiver(receiver).build());
    String token = generateJwtToken(sender);

    mockMvc
        .perform(
            get(BASE_URL + "/sent")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void getReceivedFriendRequests_Success() throws Exception {
    User sender = createTestUser("rcvd_u1", "fb-rcvd-1", "1414141414");
    User receiver = createTestUser("rcvd_u2", "fb-rcvd-2", "1515151515");
    friendRequestRepository.save(FriendRequest.builder().sender(sender).receiver(receiver).build());
    String token = generateJwtToken(receiver);

    mockMvc
        .perform(
            get(BASE_URL + "/received")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void getSentFriendRequests_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(get(BASE_URL + "/sent")).andExpect(status().isUnauthorized());
  }

  @Test
  void getReceivedFriendRequests_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(get(BASE_URL + "/received")).andExpect(status().isUnauthorized());
  }
}
