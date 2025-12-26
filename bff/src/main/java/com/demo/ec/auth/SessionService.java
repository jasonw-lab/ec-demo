package com.demo.ec.auth;

import com.demo.ec.config.AuthSessionProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final AuthSessionProperties properties;
    private final ObjectMapper objectMapper;

    public SessionService(RedisTemplate<String, String> redisTemplate,
                          AuthSessionProperties properties,
                          ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public SessionData createSession(String uid, Long userId, Instant tokenExp) throws DataAccessException {
        Instant now = Instant.now();
        long ttlSeconds = Duration.between(now, tokenExp).getSeconds();
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("Token already expired");
        }

        String sid = UUID.randomUUID().toString();
        SessionData session = new SessionData(
                sid,
                uid,
                userId,
                Collections.emptyList(),
                now,
                tokenExp
        );

        String key = properties.getPrefix() + sid;
        try {
            String payload = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, payload, ttlSeconds, TimeUnit.SECONDS);
            log.info("session_created sid={} uid={} userId={}", sid, uid, userId);
            return session;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize session", e);
        }
    }

    public SessionData getSession(String sid) throws DataAccessException {
        if (sid == null || sid.isBlank()) {
            return null;
        }
        String key = properties.getPrefix() + sid;
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            log.info("session_missing sid={}", sid);
            return null;
        }
        try {
            return objectMapper.readValue(payload, SessionData.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse session payload sid={}: {}", sid, e.getMessage());
            return null;
        }
    }

    public void deleteSession(String sid) throws DataAccessException {
        if (sid == null || sid.isBlank()) {
            return;
        }
        String key = properties.getPrefix() + sid;
        Boolean removed = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(removed)) {
            log.info("session_deleted sid={}", sid);
        } else {
            log.info("session_missing sid={}", sid);
        }
    }
}


