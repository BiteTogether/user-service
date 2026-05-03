package com.bitetogether.user.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.request.UserSearchRequest;
import com.bitetogether.user.dto.user.request.ValidateUserCriteriaRequest;
import com.bitetogether.user.enums.ValidateCriteria;
import com.bitetogether.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserControllerIntegrationTest extends BaseIntegrationTest {

  private static final String BASE_URL = "/api/v1/users";

  @Test
  void createUser_Success() throws Exception {
    User authUser = createTestUser("admin_user", "fb-admin", "1234567890");
    String token = generateJwtToken(authUser);

    CreateUserRequest request = new CreateUserRequest();
    request.setUsername("new_user123");
    request.setFirebaseUid("fb-new-user");
    request.setFullName("New User");
    request.setPhoneNumber("9876543210");
    request.setRole("USER");

    mockMvc
        .perform(
            post(BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", notNullValue()));
  }

  @Test
  void createUser_Unauthenticated_Returns401() throws Exception {
    CreateUserRequest request = new CreateUserRequest();
    request.setUsername("new_user123");
    request.setFirebaseUid("fb-new-user");
    request.setFullName("New User");
    request.setPhoneNumber("9876543210");

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateUser_Success() throws Exception {
    User user = createTestUser("test_user1", "fb-test-1", "1111111111");
    String token = generateJwtToken(user);

    UpdateUserRequest request = new UpdateUserRequest();
    request.setUsername("updated_usr");
    request.setFullName("Updated Name");

    mockMvc
        .perform(
            put(BASE_URL + "/" + user.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username", equalTo("updated_usr")));
  }

  @Test
  void updateUser_Unauthenticated_Returns401() throws Exception {
    mockMvc
        .perform(
            put(BASE_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"fullName\":\"Test\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteUser_Success() throws Exception {
    User user = createTestUser("delete_me1", "fb-del-1", "2222222222");
    String token = generateJwtToken(user);

    mockMvc
        .perform(delete(BASE_URL + "/" + user.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void deleteUser_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void getCurrentUser_Success() throws Exception {
    User user = createTestUser("current_u1", "fb-cur-1", "3333333333");
    String token = generateJwtToken(user);

    mockMvc
        .perform(get(BASE_URL + "/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username", equalTo("current_u1")));
  }

  @Test
  void getCurrentUser_Unauthenticated_Returns401() throws Exception {
    mockMvc.perform(get(BASE_URL + "/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void getUserById_Success() throws Exception {
    User user = createTestUser("getbyid_u", "fb-gbi-1", "4444444444");
    String token = generateJwtToken(user);

    mockMvc
        .perform(get(BASE_URL + "/" + user.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username", equalTo("getbyid_u")));
  }

  @Test
  void getListUserDetails_Success() throws Exception {
    User user1 = createTestUser("list_usr1", "fb-list-1", "5555555551");
    User user2 = createTestUser("list_usr2", "fb-list-2", "5555555552");
    String token = generateJwtToken(user1);

    mockMvc
        .perform(
            get(BASE_URL)
                .param("userIds", user1.getId().toString(), user2.getId().toString())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.users", hasSize(2)));
  }

  @Test
  void searchUsersWithFilter_Success() throws Exception {
    createTestUser("search_u1", "fb-search-target", "6666666661");
    User searcher = createTestUser("searcher1", "fb-searcher", "6666666660");
    String token = generateJwtToken(searcher);

    UserSearchRequest request = new UserSearchRequest();
    request.setKeyword("search_u1");

    mockMvc
        .perform(
            post(BASE_URL + "/search")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username", equalTo("search_u1")));
  }

  @Test
  void searchUsersWithFilter_BlankKeyword_Returns400() throws Exception {
    User user = createTestUser("search_u2", "fb-srch-2", "6666666667");
    String token = generateJwtToken(user);

    mockMvc
        .perform(
            post(BASE_URL + "/search")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteAvatar_Success() throws Exception {
    User user = createTestUser("avatar_u1", "fb-avt-1", "7777777777");
    user.setAvatar("http://example.com/avatar.png");
    userRepository.save(user);
    String token = generateJwtToken(user);

    mockMvc
        .perform(
            delete(BASE_URL + "/" + user.getId() + "/avatar")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void validateUserCriteria_Username_Available() throws Exception {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("available_usr1");

    mockMvc
        .perform(
            post(BASE_URL + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid", equalTo(true)));
  }

  @Test
  void validateUserCriteria_Username_Taken() throws Exception {
    createTestUser("taken_user1", "fb-taken-1", "8888888888");

    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.USERNAME);
    request.setCriteriaValue("taken_user1");

    mockMvc
        .perform(
            post(BASE_URL + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid", equalTo(false)));
  }

  @Test
  void validateUserCriteria_Phone_Available() throws Exception {
    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.PHONE);
    request.setCriteriaValue("9999999999");

    mockMvc
        .perform(
            post(BASE_URL + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid", equalTo(true)));
  }

  @Test
  void validateUserCriteria_Phone_Taken() throws Exception {
    createTestUser("phone_usr1", "fb-phone-1", "1010101010");

    ValidateUserCriteriaRequest request = new ValidateUserCriteriaRequest();
    request.setCriteriaType(ValidateCriteria.PHONE);
    request.setCriteriaValue("1010101010");

    mockMvc
        .perform(
            post(BASE_URL + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid", equalTo(false)));
  }
}
