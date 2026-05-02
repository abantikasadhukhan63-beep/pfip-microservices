package com.pfip.userservice.filter;

import com.pfip.common.dto.ValidationResponse;
import com.pfip.userservice.client.AuthServiceClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER   = "Authorization";

    private final AuthServiceClient authServiceClient;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Delegate validation to auth-service — no local JWT parsing
            ValidationResponse validation = authServiceClient.validate(token);

            if (validation != null && validation.isValid() &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                validation.getUsername(),
                                null);

                // Store userId in details for downstream use
                auth.setDetails(validation.getUserId());
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("Authenticated user: {} via auth-service",
                        validation.getUsername());
            }
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}