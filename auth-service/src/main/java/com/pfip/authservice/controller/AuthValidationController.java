package com.pfip.authservice.controller;

import com.pfip.authservice.dto.LogoutRequest;
import com.pfip.authservice.dto.ValidationRequest;
import com.pfip.authservice.dto.ValidationResponse;
import com.pfip.authservice.security.JwtUtil;
import com.pfip.authservice.service.BlacklistService;
import com.pfip.authservice.service.TokenValidationService;
import com.pfip.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthValidationController {

    private final TokenValidationService tokenValidationService;
    private final BlacklistService blacklistService;
    private final JwtUtil jwtUtil;

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse>validate(@Valid @RequestBody ValidationRequest request){
        ValidationResponse response = tokenValidationService.validate(
                request.getToken(),
                request.getCallerService() != null ? request.getCallerService(): "unknown");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request){

        try{
            String tokenId = jwtUtil.extractTokenId(request.getToken());
            LocalDateTime exp = jwtUtil.extractExpiration(request.getToken());

            blacklistService.blacklistToken(tokenId,exp);
            tokenValidationService.evictFromCaches(request.getToken());

            log.info("User logged out - token blacklisted");
            return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.ok(
                    ApiResponse.success("logged out(Token may have already expired)", null));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health(){
        return ResponseEntity.ok(ApiResponse.success("Auth service is running well", null));
    }
}
