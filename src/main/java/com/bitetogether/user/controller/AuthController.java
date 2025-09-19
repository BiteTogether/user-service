package com.bitetogether.user.controller;

import static com.bitetogether.common.util.ApiResponseUtil.buildEntityResponse;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.user.dto.auth.request.LoginRequest;
import com.bitetogether.user.dto.auth.response.LoginResponse;
import com.bitetogether.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest) {
    return buildEntityResponse(authService.login(loginRequest));
  }
}
