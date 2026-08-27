package com.example.MentorMentee.security;

import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link JwtAuthFilter} using mock servlet objects — no Spring context, no MongoDB.
 * Verifies the filter turns a valid bearer token into an authenticated {@link AuthUser} with the
 * right {@code ROLE_*} authority, and leaves the context empty for missing/invalid tokens (always
 * continuing the chain).
 */
class JwtAuthFilterTest {

    private final JwtService jwt =
            new JwtService("test-secret-value-that-is-definitely-long-enough-32b", 720);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwt);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private User mentor() {
        User u = new User();
        u.setId("m1");
        u.setEmail("mentor@example.com");
        u.setFullName("Dr. Mentor");
        u.setRole(Role.MENTOR);
        return u;
    }

    @Test
    void validTokenSetsAuthenticatedPrincipalWithRole() throws Exception {
        String token = jwt.issue(mentor());
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "expected an authentication to be set");
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MENTOR")));

        AuthUser principal = (AuthUser) auth.getPrincipal();
        assertEquals("m1", principal.id());
        assertEquals("mentor@example.com", principal.email());
        assertEquals(Role.MENTOR, principal.role());

        assertNotNull(chain.getRequest(), "filter chain should continue");
    }

    @Test
    void missingHeaderLeavesContextEmpty() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest(), "filter chain should continue even without a token");
    }

    @Test
    void invalidTokenLeavesContextEmptyButContinues() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer this.is.garbage");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void nonBearerHeaderIsIgnored() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
