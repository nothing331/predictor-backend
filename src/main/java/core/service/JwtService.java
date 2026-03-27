package core.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import core.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt-secret}")
    private String secret;

    @Value("${app.jwt-expiry-ms}")
    private long expiryMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder().setSubject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expiryMs)).signWith(key())
                .compact();
    }

    public String extractSubject(String token) {
        return validate(token).getSubject();
    }

    public Claims validate(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
