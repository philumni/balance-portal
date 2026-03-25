package com.portal.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for creating and validating JWTs.
 *
 * TOKEN STRUCTURE
 * ---------------
 * Header  : { "alg": "HS256", "typ": "JWT" }
 * Payload : {
 *     "sub"         : "jsmith",          ← username (subject)
 *     "accountNumber": "ACC-1001",
 *     "fullName"    : "John Smith",
 *     "iat"         : 1715000000,        ← issued-at  (epoch seconds)
 *     "exp"         : 1715003600         ← expires-at (iat + 1 hour)
 * }
 * Signature: HMAC-SHA256(base64(header) + "." + base64(payload), SECRET_KEY)
 *
 * SECURITY NOTE
 * -------------
 * The secret is hard-coded here for demo purposes only.
 * In production: load from an environment variable or a secrets manager,
 * never commit it to source control.
 */
public class JwtUtil {

    // ---- Configuration -------------------------------------------------------

    /** Must be >= 256 bits (32 chars) for HS256. Change this in production! */
    private static final String SECRET_STRING =
            "BalancePortalSuperSecretKey12345!";   // 32 chars = 256 bits

    private static final long EXPIRY_MS = 60 * 60 * 1000L; // 1 hour

    // ---- Derived key (built once at class-load time) -------------------------

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            SECRET_STRING.getBytes(StandardCharsets.UTF_8)
    );

    // ---- Private constructor — static utility class --------------------------
    private JwtUtil() {}

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Creates a signed JWT for the given customer.
     *
     * @param username      stored as the JWT "subject"
     * @param accountNumber stored as a custom claim
     * @param fullName      stored as a custom claim (convenience for the UI)
     * @return compact JWT string:  header.payload.signature
     */
    public static String generateToken(String username,
                                       String accountNumber,
                                       String fullName) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(username)
                .claim("accountNumber", accountNumber)
                .claim("fullName", fullName)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRY_MS))
                .signWith(SECRET_KEY)          // defaults to HS256
                .compact();
    }

    /**
     * Validates the token and returns its Claims (payload) if valid.
     *
     * @param token the compact JWT string from the Authorization header
     * @return parsed Claims
     * @throws JwtException if the token is invalid, tampered, or expired
     */
    public static Claims validateToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Convenience helper — extracts the username (subject) from a valid token.
     * Throws JwtException if the token is invalid.
     */
    public static String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Convenience helper — extracts the account number claim.
     */
    public static String getAccountNumberFromToken(String token) {
        return (String) validateToken(token).get("accountNumber");
    }
}
