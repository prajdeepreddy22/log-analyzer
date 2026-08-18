package com.loganalyzer.security;

import com.loganalyzer.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final AppProperties appProperties;

    @PostConstruct
    void validateJwtSecret() {
        getSigningKey();
    }

    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()
                        + appProperties.getJwt().getExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    public boolean isTokenValidForUserId(String token, Long userId) {
        Long tokenUserId = extractUserId(token);
        return userId != null
                && userId.equals(tokenUserId)
                && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims ->
                claims.get("userId", Long.class));
    }

    public <T> T extractClaim(String token,
                              Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Key getSigningKey() {
        byte[] keyBytes = getSecretBytes(appProperties.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] getSecretBytes(String secret) {
        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(secret);
            if (decodedBytes.length >= 32) {
                return decodedBytes;
            }
        } catch (IllegalArgumentException ex) {
            log.debug("JWT secret is not Base64 encoded; using raw secret bytes");
        }

        if (rawBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 characters when using a plain text secret, "
                            + "or decode to at least 32 bytes when using Base64");
        }

        return rawBytes;
    }
}
