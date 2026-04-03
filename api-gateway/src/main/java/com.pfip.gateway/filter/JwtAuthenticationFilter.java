package com.pfip.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX   = "Bearer ";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ID_HEADER  = "X-User-Id";

    // Paths that do NOT require a JWT token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator"
    );

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Allow public paths through without a token
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return onError(exchange, "Missing or malformed Authorization header",
                    HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.validateToken(token)) {
            return onError(exchange, "Invalid or expired JWT token",
                    HttpStatus.UNAUTHORIZED);
        }

        String username = jwtUtil.extractUsername(token);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(USERNAME_HEADER, username)
                .header(USER_ID_HEADER, "0")
                .build();

        log.debug("Gateway forwarding authenticated request for user '{}' to {}",
                username, path);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message,
                               HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
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
