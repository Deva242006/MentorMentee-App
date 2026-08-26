package com.example.MentorMentee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds {@code google.forms.*} settings for the Google Forms integration. */
@Component
@ConfigurationProperties(prefix = "google.forms")
public class GoogleProperties {

    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private int syncIntervalMinutes;

    /** True only when the feature is enabled AND client credentials are actually present. */
    public boolean isConfigured() {
        return enabled
                && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public void setSyncIntervalMinutes(int syncIntervalMinutes) { this.syncIntervalMinutes = syncIntervalMinutes; }
}
