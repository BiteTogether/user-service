package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;
import static com.bitetogether.common.util.SecurityUtils.hasRole;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.PaginationRequest;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.FriendService;
import com.bitetogether.user.util.UserHelper;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendServiceImpl implements FriendService {
  UserMapper userMapper;
  UserHelper userHelper;
  UserRepository userRepository;

  @Override
  public ApiResponse<List<FriendResponse>> getFriendsList(PaginationRequest paginationRequest) {
    Long currentUserId = getCurrentUserId();

    Pageable pageable = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize());
    Page<User> friendPage = userRepository.getFriendsByUserId(currentUserId, pageable);

    List<FriendResponse> friends = friendPage.stream().map(userMapper::toFriendResponse).toList();

    return buildApiResponse(
        ApiResponseStatus.SUCCESS,
        "Friend list retrieved successfully",
        friends,
        friendPage.getNumber(),
        friendPage.getTotalPages(),
        friendPage.getTotalElements());
  }

  @Override
  @Transactional
  public ApiResponse<String> deleteFriend(Long friendId) {
    Long currentUserId = getCurrentUserId();
    User currentUser = userHelper.findUserById(currentUserId);
    User friendToRemove = validateDeleteFriendRequest(friendId, currentUser);

    deleteFriendship(currentUser, friendToRemove);

    return buildApiResponse(ApiResponseStatus.SUCCESS, "Friend removed successfully", null);
  }

  private User validateDeleteFriendRequest(Long friendId, User currentUser) {
    User friendUser = userHelper.findFriendById(friendId);

    if (hasRole(Role.USER.name())) {
      boolean isFriended = currentUser.getFriends().contains(friendUser);
      if (!isFriended) {
        throw new AppException(ErrorCode.FRIEND_NOT_FOUND);
      }
    }

    return friendUser;
  }

  void deleteFriendship(User user1, User user2) {
    user1.getFriends().remove(user2);
    user2.getFriends().remove(user1);

    userHelper.saveUser(user1);
    userHelper.saveUser(user2);
  }
}
