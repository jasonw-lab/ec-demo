package com.demo.ec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.session")
public class AuthSessionProperties {
    /**
     * Redis key prefix for session entries.
     */
    private String prefix = "auth:session:";

    /**
     * Cookie name for the session id.
     */
    private String cookieName = "sid";

    /**
     * Whether to mark cookie as Secure.
     */
    private boolean secure = true;

    /**
     * SameSite attribute for the cookie (None/Lax/Strict).
     */
    private String sameSite = "Lax";

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }
}


