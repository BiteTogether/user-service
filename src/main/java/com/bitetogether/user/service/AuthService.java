package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;

public interface AuthService {
    ApiResponse<String> login(LoginRequest loginRequest);
}
