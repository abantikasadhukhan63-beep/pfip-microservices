package com.pfip.userservice.controller;

import com.pfip.common.dto.ApiResponse;
import com.pfip.userservice.dto.AuthDtos;
import com.pfip.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user and return a JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {

        log.info("Registration request for username: {}", request.getUsername());
        AuthDtos.AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * POST /api/auth/login
     * Authenticate and return a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {

        log.info("Login request for username: {}", request.getUsername());
        AuthDtos.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}