package com.bitetogether.user.service.impl;

import static com.bitetogether.common.util.ApiResponseUtil.buildApiResponse;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.enums.UserRole;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
  UserRepository userRepository;
  UserMapper userMapper;
  PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public ApiResponse<Long> createUser(CreateUserRequest createUserRequest) {
    validateCreateUserRequest(createUserRequest);

    User newUser = userMapper.toEntity(createUserRequest);

    handlePassword(newUser);
    handleRole(newUser);

    User databaseUser = userRepository.save(newUser);

    return buildApiResponse(
        ApiResponseStatus.SUCCESS, "User created successfully", databaseUser.getId());
  }

  private void validateCreateUserRequest(CreateUserRequest createUserRequest) {
    if (userRepository.existsByEmail(createUserRequest.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }
    if (userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())) {
      throw new AppException(ErrorCode.PHONE_EXISTED);
    }
  }

  private void handlePassword(User newUser) {
    String encodedPassword = passwordEncoder.encode(newUser.getPassword());
    newUser.setPassword(encodedPassword);
  }

  private void handleRole(User newUser) {
    if (newUser.getRole() == null) {
      newUser.setRole(UserRole.valueOf("USER"));
    }
  }
}
