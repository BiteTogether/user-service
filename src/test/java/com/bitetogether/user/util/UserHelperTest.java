package com.bitetogether.user.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserHelperTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserHelper userHelper;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder().id(1L).username("testuser").email("test@example.com").build();
  }

  @Test
  void findUserById_WithExistingUser_ReturnsUser() {
    Long userId = 1L;

    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    User result = userHelper.findUserById(userId);

    assertNotNull(result);
    assertEquals(testUser.getId(), result.getId());
    assertEquals(testUser.getUsername(), result.getUsername());
    assertEquals(testUser.getEmail(), result.getEmail());

    verify(userRepository, times(1)).findById(userId);
  }

  @Test
  void findUserById_WithNonExistingUser_ThrowsUserNotFoundException() {
    Long userId = 999L;

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    AppException exception =
        assertThrows(AppException.class, () -> userHelper.findUserById(userId));

    assertNotNull(exception);
    verify(userRepository, times(1)).findById(userId);
  }

  @Test
  void findFriendById_WithExistingFriend_ReturnsFriend() {
    Long friendId = 2L;
    User friend =
        User.builder().id(friendId).username("frienduser").email("friend@example.com").build();

    when(userRepository.findById(friendId)).thenReturn(Optional.of(friend));

    User result = userHelper.findFriendById(friendId);

    assertNotNull(result);
    assertEquals(friend.getId(), result.getId());
    assertEquals(friend.getUsername(), result.getUsername());
    assertEquals(friend.getEmail(), result.getEmail());

    verify(userRepository, times(1)).findById(friendId);
  }

  @Test
  void findFriendById_WithNonExistingFriend_ThrowsFriendNotFoundException() {
    Long friendId = 999L;

    when(userRepository.findById(friendId)).thenReturn(Optional.empty());

    AppException exception =
        assertThrows(AppException.class, () -> userHelper.findFriendById(friendId));

    assertNotNull(exception);
    verify(userRepository, times(1)).findById(friendId);
  }

  @Test
  void saveUser_WithValidUser_ReturnsSavedUser() {
    when(userRepository.save(testUser)).thenReturn(testUser);

    User result = userHelper.saveUser(testUser);

    assertNotNull(result);
    assertEquals(testUser.getId(), result.getId());
    assertEquals(testUser.getUsername(), result.getUsername());
    assertEquals(testUser.getEmail(), result.getEmail());

    verify(userRepository, times(1)).save(testUser);
  }

  @Test
  void saveUser_WithUpdatedUser_ReturnsSavedUser() {
    User updatedUser =
        User.builder().id(1L).username("updateduser").email("updated@example.com").build();

    when(userRepository.save(updatedUser)).thenReturn(updatedUser);

    User result = userHelper.saveUser(updatedUser);

    assertNotNull(result);
    assertEquals(updatedUser.getId(), result.getId());
    assertEquals(updatedUser.getUsername(), result.getUsername());
    assertEquals(updatedUser.getEmail(), result.getEmail());

    verify(userRepository, times(1)).save(updatedUser);
  }

  @Test
  void saveUser_WithNewUser_ReturnsSavedUser() {
    User newUser = User.builder().id(null).username("newuser").email("new@example.com").build();
    User savedUser = User.builder().id(10L).username("newuser").email("new@example.com").build();

    when(userRepository.save(newUser)).thenReturn(savedUser);

    User result = userHelper.saveUser(newUser);

    assertNotNull(result);
    assertNotNull(result.getId());
    assertEquals(10L, result.getId());
    assertEquals(newUser.getUsername(), result.getUsername());
    assertEquals(newUser.getEmail(), result.getEmail());

    verify(userRepository, times(1)).save(newUser);
  }
}
