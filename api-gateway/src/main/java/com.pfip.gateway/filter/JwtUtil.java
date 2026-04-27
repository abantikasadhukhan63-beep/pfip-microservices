package com.pfip.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.function.Function;

/**
 * Stateless JWT helper used by the reactive gateway filter.
 * Only validation and claim extraction — token generation lives in user-service.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Gateway JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Gateway JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Gateway JWT malformed: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Gateway JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    public String extractuserId(String token) {
        return extractClaim(token, claims -> String.valueOf(claims.get("userId")));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}