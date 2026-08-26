package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * OAuth2 tokens for a mentor's linked Google account, used to call the Forms API on their behalf.
 * NOTE: tokens are stored as-is for simplicity; encrypt them at rest for a production deployment.
 */
@Document(collection = "googleAccounts")
public class GoogleAccount {

    @Id
    private String id;

    @Indexed(unique = true)
    private String mentorId;

    private String email;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private String scope;
    private Instant connectedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Instant getConnectedAt() { return connectedAt; }
    public void setConnectedAt(Instant connectedAt) { this.connectedAt = connectedAt; }
}
