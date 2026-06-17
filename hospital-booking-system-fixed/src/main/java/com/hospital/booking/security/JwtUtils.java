package com.hospital.booking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Component @Slf4j
public class JwtUtils {

    @Value("${jwt.secret}") private String jwtSecret;

    // BUG FIX: Was int — silent overflow for values > Integer.MAX_VALUE (~25 days in ms)
    @Value("${jwt.expiration}") private long jwtExpirationMs;
    @Value("${jwt.refresh-expiration}") private long jwtRefreshExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateJwtToken(Authentication auth) {
        UserDetailsImpl u = (UserDetailsImpl) auth.getPrincipal();
        return buildToken(u.getUsername(), u.getId(), jwtExpirationMs, Map.of());
    }

    public String generateTokenFromUsername(String username, Long userId) {
        return buildToken(username, userId, jwtExpirationMs, Map.of());
    }

    public String generateRefreshToken(String username, Long userId) {
        return buildToken(username, userId, jwtRefreshExpirationMs, Map.of("type", "refresh"));
    }

    private String buildToken(String subject, Long userId, long expiryMs, Map<String, Object> extra) {
        Map<String, Object> claims = new HashMap<>(extra);
        claims.put("userId", userId);
        return Jwts.builder()
            .claims(claims).subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiryMs))
            .signWith(key(), Jwts.SIG.HS256)
            .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }

    public Long getUserIdFromJwtToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload().get("userId", Long.class);
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
}
