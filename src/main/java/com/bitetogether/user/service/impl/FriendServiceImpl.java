package com.bitetogether.user.service.impl;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.Role;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.ErrorCode;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.FriendService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;
import static com.bitetogether.common.util.SecurityUtils.getCurrentUserId;
import static com.bitetogether.common.util.SecurityUtils.hasRole;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendServiceImpl implements FriendService {
    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public ApiResponse<List<FriendResponse>> getFriendsList() {
        Long currentUserId = getCurrentUserId();

        User currentUser = findUserById(currentUserId);
        List<User> friends = currentUser.getFriendList();

        List<FriendResponse> friendResponseList = friends.stream()
                .map(userMapper::toFriendResponse)
                .collect(Collectors.toList());

        return buildApiResponse(
                ApiResponseStatus.SUCCESS, "Friend list retrieved successfully", friendResponseList);
    }

    @Override
    @Transactional
    public ApiResponse<String> deleteFriend(Long friendId) {
        Long currentUserId = getCurrentUserId();
        User currentUser = findUserById(currentUserId);
        User friendToRemove = validateDeleteFriendRequest(friendId, currentUser);

        currentUser.getFriendList().remove(friendToRemove);
        friendToRemove.getFriendList().remove(currentUser);

        userRepository.save(currentUser);
        userRepository.save(friendToRemove);

        return buildApiResponse(
                ApiResponseStatus.SUCCESS, "Friend removed successfully", null
        );
    }

    private User findUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User validateDeleteFriendRequest(Long friendId, User currentUser) {
        User friendUser = findUserById(friendId);

        if (hasRole(Role.USER.name())) {
            boolean areFriends = currentUser.getFriendList().stream()
                    .anyMatch(friend -> friend.getId().equals(friendId));

            if (!areFriends) {
                throw new AppException(ErrorCode.FRIEND_NOT_FOUND);
            }
        }

        return friendUser;
    }
}
