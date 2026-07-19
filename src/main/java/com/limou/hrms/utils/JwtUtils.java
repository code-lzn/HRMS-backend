package com.limou.hrms.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 — 生成和校验 Token。
 * <p>
 * Token 中仅存储 userId / userName / userRole 三个轻量字段，
 * 用于 LoginInterceptor 在无 Session 时还原用户身份。
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据用户信息生成 JWT Token
     */
    public String generateToken(Long userId, String userName, String userRole) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("userName", userName)
                .claim("userRole", userRole)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Token 中解析 Claims，校验失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }
}
