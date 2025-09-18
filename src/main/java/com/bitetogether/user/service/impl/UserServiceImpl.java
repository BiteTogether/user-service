package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.exception.ErrorCode;
import com.bitetogether.user.convert.UserMapper;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.model.User;
import com.bitetogether.user.repository.UserRepository;
import com.bitetogether.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
  UserRepository userRepository;
  UserMapper userMapper;

  @Override
  @Transactional
  public ApiResponse<Long> createUser(CreateUserRequest createUserRequest) {
    if (userRepository.existsByEmail(createUserRequest.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }
    if (userRepository.existsByUsername(createUserRequest.getUsername())) {
      throw new AppException(ErrorCode.USERNAME_EXISTED);
    }
    User newUser = userMapper.toEntity(createUserRequest);
    User databaseUser = userRepository.save(newUser);
    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User created successfully", databaseUser.getId());
  }
}
