package com.pfip.transactionservice.client;

import com.pfip.common.dto.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class AuthServiceClient {

    private final WebClient webClient;

    public AuthServiceClient(
            @Value("${auth.service.url:http://localhost:8083}") String authServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    public ValidationResponse validate(String token) {
        try {
            return webClient.post()
                    .uri("/auth/validate")
                    .bodyValue(Map.of(
                            "token", token,
                            "callerService", "user-service"))
                    .retrieve()
                    .bodyToMono(ValidationResponse.class)
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(e -> {
                        log.error("Auth service unreachable: {}", e.getMessage());
                        return Mono.just(ValidationResponse.builder()
                                .valid(false)
                                .reason("Auth service unavailable")
                                .build());
                    })
                    .block();
        } catch (Exception e) {
            log.error("Auth service call failed: {}", e.getMessage());
            return ValidationResponse.builder()
                    .valid(false)
                    .reason("Auth service call failed")
                    .build();
        }
    }
}