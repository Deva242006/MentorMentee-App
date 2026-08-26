package com.example.MentorMentee.service;

import com.example.MentorMentee.config.GoogleProperties;
import com.example.MentorMentee.dto.FormDtos.GoogleStatusView;
import com.example.MentorMentee.model.GoogleAccount;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.GoogleAccountRepository;
import com.example.MentorMentee.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hand-rolled Google OAuth2 authorization-code flow (no session): the {@code state} parameter is an
 * HMAC-signed token carrying the mentor id, so the public callback can attribute the grant without a
 * server session. Tokens are stored per mentor and refreshed on demand.
 */
@Service
public class GoogleOAuthService {

    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final List<String> SCOPES = List.of(
            "openid", "email",
            "https://www.googleapis.com/auth/forms.body",
            "https://www.googleapis.com/auth/forms.responses.readonly");
    private static final long STATE_TTL_SECONDS = 900; // 15 minutes

    private final GoogleProperties props;
    private final GoogleAccountRepository accounts;
    private final UserRepository users;
    private final byte[] stateSecret;
    private final RestClient http = RestClient.create();

    public GoogleOAuthService(GoogleProperties props, GoogleAccountRepository accounts, UserRepository users,
                              @Value("${app.jwt.secret}") String secret) {
        this.props = props;
        this.accounts = accounts;
        this.users = users;
        this.stateSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public GoogleStatusView status(String mentorId) {
        Optional<GoogleAccount> acc = accounts.findByMentorId(mentorId);
        return new GoogleStatusView(props.isConfigured(), acc.isPresent(), acc.map(GoogleAccount::getEmail).orElse(null));
    }

    /** Builds the Google consent URL the mentor's browser is sent to. */
    public String buildAuthUrl(String mentorId) {
        requireConfigured();
        String scope = String.join(" ", SCOPES);
        return AUTH_ENDPOINT
                + "?client_id=" + enc(props.getClientId())
                + "&redirect_uri=" + enc(props.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + enc(scope)
                + "&access_type=offline"
                + "&include_granted_scopes=true"
                + "&prompt=consent"
                + "&state=" + enc(signState(mentorId));
    }

    /** Exchanges the callback code for tokens and links the mentor's Google account. */
    public void handleCallback(String code, String state) {
        requireConfigured();
        String mentorId = verifyState(state);
        User mentor = users.findById(mentorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mentor in state"));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("redirect_uri", props.getRedirectUri());
        form.add("grant_type", "authorization_code");
        Map<String, Object> token = post(TOKEN_ENDPOINT, form);

        String accessToken = string(token, "access_token");
        String refreshToken = string(token, "refresh_token");
        long expiresIn = number(token, "expires_in", 3600);
        String scope = string(token, "scope");
        String email = fetchEmail(accessToken);

        GoogleAccount acc = accounts.findByMentorId(mentorId).orElseGet(GoogleAccount::new);
        acc.setMentorId(mentorId);
        acc.setEmail(email);
        acc.setAccessToken(accessToken);
        // Google only returns a refresh token on the first consent; keep the old one otherwise.
        if (refreshToken != null && !refreshToken.isBlank()) {
            acc.setRefreshToken(refreshToken);
        }
        acc.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        acc.setScope(scope);
        accounts.save(acc);

        mentor.setGoogleConnected(true);
        users.save(mentor);
    }

    public void disconnect(String mentorId) {
        accounts.deleteByMentorId(mentorId);
        users.findById(mentorId).ifPresent(u -> {
            u.setGoogleConnected(false);
            users.save(u);
        });
    }

    /** Returns a valid access token for the mentor, refreshing it if it has (nearly) expired. */
    public String validAccessToken(String mentorId) {
        GoogleAccount acc = accounts.findByMentorId(mentorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Connect your Google account first"));
        boolean expired = acc.getExpiresAt() == null
                || Instant.now().isAfter(acc.getExpiresAt().minusSeconds(60));
        if (!expired) {
            return acc.getAccessToken();
        }
        if (acc.getRefreshToken() == null || acc.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Google session expired — please reconnect your account");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", acc.getRefreshToken());
        form.add("grant_type", "refresh_token");
        Map<String, Object> token = post(TOKEN_ENDPOINT, form);

        acc.setAccessToken(string(token, "access_token"));
        acc.setExpiresAt(Instant.now().plusSeconds(number(token, "expires_in", 3600)));
        accounts.save(acc);
        return acc.getAccessToken();
    }

    // --- helpers ---

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Forms integration is not configured on this server");
        }
    }

    private String fetchEmail(String accessToken) {
        try {
            Map<String, Object> info = http.get().uri(USERINFO_ENDPOINT)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return info == null ? null : string(info, "email");
        } catch (RestClientException e) {
            return null; // email is best-effort
        }
    }

    private Map<String, Object> post(String url, MultiValueMap<String, String> form) {
        try {
            return http.post().uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google token request failed: " + e.getMessage());
        }
    }

    private static String string(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private static long number(Map<String, Object> m, String key, long fallback) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : fallback;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // --- signed state (mentorId + expiry, HMAC-SHA256) ---

    private String signState(String mentorId) {
        String payload = mentorId + ":" + (Instant.now().getEpochSecond() + STATE_TTL_SECONDS);
        String p = b64(payload.getBytes(StandardCharsets.UTF_8));
        return p + "." + b64(hmac(p));
    }

    private String verifyState(String state) {
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing state");
        }
        String[] parts = state.split("\\.");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed state");
        }
        byte[] expected = hmac(parts[0]);
        byte[] actual;
        try {
            actual = java.util.Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad state signature");
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state signature");
        }
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        int sep = payload.lastIndexOf(':');
        String mentorId = payload.substring(0, sep);
        long exp = Long.parseLong(payload.substring(sep + 1));
        if (Instant.now().getEpochSecond() > exp) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection request expired — please retry");
        }
        return mentorId;
    }

    private static String b64(byte[] data) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stateSecret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }
}
