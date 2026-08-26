package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.FormDtos.ConnectResponse;
import com.example.MentorMentee.dto.FormDtos.GoogleStatusView;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.GoogleOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Google account linking for the Forms feature. The mentor-scoped endpoints require MENTOR; the
 * OAuth callback is public (hit by Google's browser redirect, carrying a signed state) and 302s back
 * to the frontend.
 */
@RestController
public class GoogleController {

    private final GoogleOAuthService oauth;
    private final String frontendBaseUrl;

    public GoogleController(GoogleOAuthService oauth,
                            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.oauth = oauth;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping("/api/mentor/google/status")
    public GoogleStatusView status(@AuthenticationPrincipal AuthUser me) {
        return oauth.status(me.id());
    }

    @GetMapping("/api/mentor/google/connect")
    public ConnectResponse connect(@AuthenticationPrincipal AuthUser me) {
        return new ConnectResponse(oauth.buildAuthUrl(me.id()));
    }

    @DeleteMapping("/api/mentor/google")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@AuthenticationPrincipal AuthUser me) {
        oauth.disconnect(me.id());
    }

    /** Public: Google redirects the mentor's browser here after consent. */
    @GetMapping("/api/google/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        String target;
        try {
            if (error != null || code == null) {
                throw new IllegalStateException(error == null ? "Missing authorization code" : error);
            }
            oauth.handleCallback(code, state);
            target = frontendBaseUrl + "/mentor/forms?google=connected";
        } catch (Exception e) {
            target = frontendBaseUrl + "/mentor/forms?google=error";
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
