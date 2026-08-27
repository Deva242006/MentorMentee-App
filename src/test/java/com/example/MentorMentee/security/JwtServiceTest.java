package com.example.MentorMentee.security;

import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.security.JwtService.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the hand-rolled HS256 JWT (no Spring context, no MongoDB). This is the riskiest
 * bespoke code in the app — the HMAC signing and the tiny JSON reader/writer — so it's covered
 * directly: round-trip, escaping, and every rejection path.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-value-that-is-definitely-long-enough-32b";

    private JwtService service(long ttlMinutes) {
        return new JwtService(SECRET, ttlMinutes);
    }

    private User user(String id, String email, String name, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        return u;
    }

    @Test
    void issueThenParseRoundTripsAllClaims() {
        JwtService jwt = service(720);
        String token = jwt.issue(user("u1", "mentor@example.com", "Dr. Mentor", Role.MENTOR));

        Claims c = jwt.parse(token);

        assertEquals("u1", c.userId());
        assertEquals("mentor@example.com", c.email());
        assertEquals("Dr. Mentor", c.name());
        assertEquals("MENTOR", c.role());
    }

    @Test
    void tokenHasThreeBase64UrlParts() {
        String token = service(720).issue(user("u1", "a@b.co", "A", Role.ADMIN));
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        // base64url uses -/_ and no padding; must not contain +, /, or =
        assertTrue(parts[1].chars().noneMatch(ch -> ch == '+' || ch == '/' || ch == '='));
    }

    @Test
    void escapesSpecialCharactersInNameAndRoundTrips() {
        JwtService jwt = service(720);
        String tricky = "O'Brien \"Quote\" \\slash\\ \n newline \t tab é ✓";
        String token = jwt.issue(user("u2", "x@y.z", tricky, Role.MENTEE));

        Claims c = jwt.parse(token);
        assertEquals(tricky, c.name());
    }

    @Test
    void rejectsTamperedPayload() {
        JwtService jwt = service(720);
        String token = jwt.issue(user("u1", "a@b.co", "A", Role.MENTOR));
        String[] parts = token.split("\\.");

        // Flip a character in the payload; the signature no longer matches.
        String badChar = parts[1].startsWith("A") ? "B" : "A";
        String tampered = parts[0] + "." + badChar + parts[1].substring(1) + "." + parts[2];

        assertThrows(JwtService.JwtException.class, () -> jwt.parse(tampered));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = service(720).issue(user("u1", "a@b.co", "A", Role.MENTOR));
        JwtService other = new JwtService("a-completely-different-secret-key-value-here-32bytes", 720);
        assertThrows(JwtService.JwtException.class, () -> other.parse(token));
    }

    @Test
    void rejectsExpiredToken() {
        // Negative TTL => exp is in the past the instant it's issued.
        JwtService jwt = service(-1);
        String token = jwt.issue(user("u1", "a@b.co", "A", Role.MENTOR));
        JwtService.JwtException ex =
                assertThrows(JwtService.JwtException.class, () -> jwt.parse(token));
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    void rejectsMalformedToken() {
        JwtService jwt = service(720);
        assertThrows(JwtService.JwtException.class, () -> jwt.parse("not-a-jwt"));
        assertThrows(JwtService.JwtException.class, () -> jwt.parse("only.two"));
    }

    @Test
    void nullClaimSurvivesRoundTrip() {
        // A user with no id yet (e.g. never persisted) still produces a parseable token.
        JwtService jwt = service(720);
        String token = jwt.issue(user(null, "a@b.co", "A", Role.ADMIN));
        Claims c = jwt.parse(token);
        assertNotNull(c);
        assertEquals("ADMIN", c.role());
    }
}
