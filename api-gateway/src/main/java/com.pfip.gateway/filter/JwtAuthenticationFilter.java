package com.pfip.gateway.filter;

import com.pfip.common.dto.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX   = "Bearer ";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ID_HEADER  = "X-User-Id";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator"
    );

    private final WebClient webClient;

    public JwtAuthenticationFilter(
            @Value("${auth.service.url:http://localhost:8083}") String authServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Allow public paths through immediately
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Call auth-service reactively
        return webClient.post()
                .uri("/auth/validate")
                .bodyValue(Map.of("token", token, "callerService", "api-gateway"))
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .timeout(Duration.ofSeconds(3))
                .flatMap(validation -> {
                    if (validation == null || !validation.isValid()) {
                        String reason = validation != null
                                ? validation.getReason()
                                : "Unknown";
                        log.warn("Invalid token at gateway: {}", reason);
                        return onError(exchange, "Invalid or expired token",
                                HttpStatus.UNAUTHORIZED);
                    }

                    // Inject verified identity headers
                    String userId   = validation.getUserId() != null
                            ? validation.getUserId().toString() : "0";
                    String username = validation.getUsername() != null
                            ? validation.getUsername() : "";

                    log.debug("Gateway authenticated user: {} for path: {}", username, path);

                    var mutatedRequest = exchange.getRequest().mutate()
                            .header(USERNAME_HEADER, username)
                            .header(USER_ID_HEADER, userId)
                            .build();

                    return chain.filter(exchange.mutate()
                            .request(mutatedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Auth service unreachable from gateway: {}", e.getMessage());
                    return onError(exchange, "Authentication service unavailable",
                            HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> onError(ServerWebExchange exchange,
                               String message, HttpStatus status) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"success\":false,\"error\":\"%s\",\"status\":%d}",
                message, status.value());
        var buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}