package com.demo.ec.bff.application.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class SessionData {
    private final String sid;
    private final String uid;
    private final Long userId;
    private final List<String> roles;
    private final Instant createdAt;
    private final Instant tokenExp;

    @JsonCreator
    public SessionData(
            @JsonProperty("sid") String sid,
            @JsonProperty("uid") String uid,
            @JsonProperty("userId") Long userId,
            @JsonProperty("roles") List<String> roles,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("tokenExp") Instant tokenExp) {
        this.sid = sid;
        this.uid = uid;
        this.userId = userId;
        this.roles = roles;
        this.createdAt = createdAt;
        this.tokenExp = tokenExp;
    }

    public String getSid() {
        return sid;
    }

    public String getUid() {
        return uid;
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getTokenExp() {
        return tokenExp;
    }
}


