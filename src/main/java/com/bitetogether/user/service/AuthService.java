package com.bitetogether.user.service;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.response.LoginResponse;

public interface AuthService {
  ApiResponse<LoginResponse> login(LoginRequest loginRequest);
}
