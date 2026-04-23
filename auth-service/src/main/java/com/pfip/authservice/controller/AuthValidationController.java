package com.pfip.authservice.controller;

import com.pfip.authservice.service.BlacklistService;
import com.pfip.authservice.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthValidationController {

    private final TokenValidationService tokenValidationService;
    private final BlacklistService blacklistService;
}
