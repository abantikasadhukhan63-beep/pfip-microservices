
package com.pfip.userservice.controller;

import com.pfip.common.dto.ApiResponse;
import com.pfip.userservice.dto.AuthDtos;
import com.pfip.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDtos.UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        AuthDtos.UserDto user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * GET /api/users/{id}
     * Get a user by ID (authenticated users can view any profile).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthDtos.UserDto>> getUserById(@PathVariable Long id) {
        AuthDtos.UserDto user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * GET /api/users
     * Get all users — ADMIN only.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthDtos.UserDto>>> getAllUsers() {
        List<AuthDtos.UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * PATCH /api/users/{id}/toggle
     * Enable or disable a user — ADMIN only.
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id) {
        userService.toggleUserEnabled(id);
        return ResponseEntity.ok(ApiResponse.success("User status toggled", null));
    }
}