package com.bitetogether.user.service.impl;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.bitetogether.common.util.ApiResponseUtil.buildResponse;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    @Override
    public ApiResponse<String> login(LoginRequest loginRequest) {
        //TODO: Implement actual login logic
        return buildResponse(
                ApiResponseStatus.SUCCESS,
                "Login successful",
                null
                );
    }
}

