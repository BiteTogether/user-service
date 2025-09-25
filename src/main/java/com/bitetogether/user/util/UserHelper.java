package com.bitetogether.user.util;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserHelper {

    UserRepository userRepository;

    public User findUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public User findFriendById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_NOT_FOUND));
    }
}
