package com.ict.lms.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ict.lms.model.AppUser;
import com.ict.lms.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Creates and reads signed JWT tokens. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationHours * 3600_000L;
    }

    /** Build a token that identifies this user. */
    public String generate(AppUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("grade", user.getGrade())
                .claim("name", user.getFullName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Verify a token and turn it back into an AuthUser. Throws if invalid/expired. */
    public AuthUser parse(String token) {
        Claims c = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object uidRaw = c.get("uid");
        Long uid = (uidRaw == null) ? null : ((Number) uidRaw).longValue();

        Object gradeRaw = c.get("grade");
        Integer grade = (gradeRaw == null) ? null : ((Number) gradeRaw).intValue();

        Role role = Role.valueOf(c.get("role", String.class));
        String name = c.get("name", String.class);

        return new AuthUser(uid, c.getSubject(), role, grade, name);
    }
}
