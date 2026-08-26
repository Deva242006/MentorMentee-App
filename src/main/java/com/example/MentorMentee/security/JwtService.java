package com.example.MentorMentee.security;

import com.example.MentorMentee.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal HS256 (HMAC-SHA256) JWT implementation using only the JDK, so the app needs no external JWT
 * library and is not coupled to any particular JSON library version. Tokens are a standard
 * {@code header.payload.signature} triple (base64url, unpadded) and carry the user id (sub), email,
 * name and role. The payload is a small flat JSON object we fully control, so the tiny reader/writer
 * below is sufficient (and escape-aware for the free-form name/email fields).
 */
@Service
public class JwtService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER_B64 =
            B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final byte[] secret;
    private final long ttlSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-minutes:720}") long ttlMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlMinutes * 60L;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        StringBuilder json = new StringBuilder(256).append('{');
        writeString(json, "sub", user.getId()).append(',');
        writeString(json, "email", user.getEmail()).append(',');
        writeString(json, "name", user.getFullName()).append(',');
        writeString(json, "role", user.getRole().name()).append(',');
        json.append("\"iat\":").append(now.getEpochSecond()).append(',');
        json.append("\"exp\":").append(now.plusSeconds(ttlSeconds).getEpochSecond());
        json.append('}');

        String payloadB64 = B64.encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
        String signingInput = HEADER_B64 + "." + payloadB64;
        return signingInput + "." + B64.encodeToString(hmac(signingInput));
    }

    public Claims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("Malformed token");
        }
        byte[] expected = hmac(parts[0] + "." + parts[1]);
        byte[] actual;
        try {
            actual = B64D.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Bad signature encoding");
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new JwtException("Bad signature");
        }
        try {
            Map<String, Object> payload = parseFlatObject(
                    new String(B64D.decode(parts[1]), StandardCharsets.UTF_8));
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new JwtException("Token expired");
            }
            return new Claims(
                    str(payload.get("sub")),
                    str(payload.get("email")),
                    str(payload.get("name")),
                    str(payload.get("role")));
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Unreadable token");
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    // --- minimal JSON (flat object of string/number values only) ---

    /** Appends {@code "key":"value"} with the value JSON-escaped. */
    private static StringBuilder writeString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) {
            return sb.append("null");
        }
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"');
    }

    /**
     * Parses a flat JSON object whose values are strings or integers. Sufficient for our own token
     * payload (not a general-purpose parser). Throws {@link JwtException} on anything unexpected.
     */
    private static Map<String, Object> parseFlatObject(String json) {
        Map<String, Object> out = new HashMap<>();
        int i = skipWs(json, 0);
        i = expect(json, i, '{');
        i = skipWs(json, i);
        if (i < json.length() && json.charAt(i) == '}') {
            return out;
        }
        while (true) {
            i = skipWs(json, i);
            StringBuilder key = new StringBuilder();
            i = readString(json, i, key);
            i = skipWs(json, i);
            i = expect(json, i, ':');
            i = skipWs(json, i);
            if (i >= json.length()) {
                throw new JwtException("Truncated token payload");
            }
            char c = json.charAt(i);
            if (c == '"') {
                StringBuilder val = new StringBuilder();
                i = readString(json, i, val);
                out.put(key.toString(), val.toString());
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                int start = i;
                if (c == '-') {
                    i++;
                }
                while (i < json.length() && Character.isDigit(json.charAt(i))) {
                    i++;
                }
                out.put(key.toString(), Long.parseLong(json.substring(start, i)));
            } else if (json.startsWith("null", i)) {
                out.put(key.toString(), null);
                i += 4;
            } else {
                throw new JwtException("Unexpected value in token payload");
            }
            i = skipWs(json, i);
            if (i >= json.length()) {
                throw new JwtException("Truncated token payload");
            }
            char sep = json.charAt(i++);
            if (sep == '}') {
                return out;
            }
            if (sep != ',') {
                throw new JwtException("Malformed token payload");
            }
        }
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int expect(String s, int i, char c) {
        if (i >= s.length() || s.charAt(i) != c) {
            throw new JwtException("Expected '" + c + "' in token payload");
        }
        return i + 1;
    }

    /** Reads a JSON string starting at the opening quote; appends the decoded value to {@code out}. */
    private static int readString(String s, int i, StringBuilder out) {
        i = expect(s, i, '"');
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') {
                return i;
            }
            if (c == '\\') {
                if (i >= s.length()) {
                    break;
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"'  -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/'  -> out.append('/');
                    case 'b'  -> out.append('\b');
                    case 'f'  -> out.append('\f');
                    case 'n'  -> out.append('\n');
                    case 'r'  -> out.append('\r');
                    case 't'  -> out.append('\t');
                    case 'u'  -> {
                        if (i + 4 > s.length()) {
                            throw new JwtException("Bad unicode escape in token payload");
                        }
                        out.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw new JwtException("Bad escape in token payload");
                }
            } else {
                out.append(c);
            }
        }
        throw new JwtException("Unterminated string in token payload");
    }

    public record Claims(String userId, String email, String name, String role) {
    }

    public static class JwtException extends RuntimeException {
        public JwtException(String message) {
            super(message);
        }
    }
}
