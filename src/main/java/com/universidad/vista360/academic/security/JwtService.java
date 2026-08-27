package com.universidad.vista360.academic.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private final SecretKey key;
    private final String issuer;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public UserPrincipal parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String roleClaim = claims.get("role", String.class);
        if (roleClaim == null) {
            throw new JwtException("Token is missing the 'role' claim");
        }
        UserPrincipal.Role role = UserPrincipal.Role.valueOf(roleClaim);
        String studentId = claims.get("studentId", String.class);

        return new UserPrincipal(claims.getSubject(), role, studentId);
    }

    public String generateDemoToken(String subject, UserPrincipal.Role role, String studentId, Duration ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        if (studentId != null) {
            builder.claim("studentId", studentId);
        }
        return builder.signWith(key).compact();
    }
}
