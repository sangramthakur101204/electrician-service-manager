package com.electrician.servicemanager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Secret key — 256 bit minimum for HS256
    private static final String SECRET = "ElectroServeSecretKey2025ForJWTTokenGenerationMatoshreeEnterprises";
    private static final long EXPIRY = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Token generate karo
    public String generateToken(Long userId, String mobile, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("mobile", mobile)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Token se userId nikalo
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    // Token se role nikalo
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // Token valid hai?
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
