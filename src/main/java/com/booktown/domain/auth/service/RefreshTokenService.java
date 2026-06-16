package com.booktown.domain.auth.service;

import com.booktown.global.exception.CustomException;
import com.booktown.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, long expirationMs) {
        redisTemplate.opsForValue()
                .set(key(userId), refreshToken, Duration.ofMillis(expirationMs));
    }

    public void validate(Long userId, String refreshToken) {
        String savedToken = redisTemplate.opsForValue().get(key(userId));
        if (savedToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!savedToken.equals(refreshToken)) {
            redisTemplate.delete(key(userId));
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
        }
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
