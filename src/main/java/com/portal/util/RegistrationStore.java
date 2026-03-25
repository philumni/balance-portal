package com.portal.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory store for pending (unverified) registrations.
 *
 * In production this would be a database table with a verified boolean column,
 * or a Redis cache with a TTL matching the token expiry.
 *
 * KEY DESIGN
 * ----------
 * We keep two maps:
 *   tokenMap    : verificationToken → PendingRegistration
 *   usernameMap : username (lower)  → verificationToken
 *   emailMap    : email (lower)     → verificationToken
 *
 * The username/email maps let us quickly check for duplicates on registration.
 */
public class RegistrationStore {

    private static final RegistrationStore INSTANCE = new RegistrationStore();
    public  static RegistrationStore getInstance() { return INSTANCE; }

    private final Map<String, PendingRegistration> tokenMap    = new ConcurrentHashMap<>();
    private final Map<String, String>              usernameMap = new ConcurrentHashMap<>();
    private final Map<String, String>              emailMap    = new ConcurrentHashMap<>();

    private RegistrationStore() {}

    // -------------------------------------------------------------------------
    // Store a new pending registration
    // -------------------------------------------------------------------------

    /**
     * Creates and stores a PendingRegistration.
     * Returns the generated verification token so the servlet can build the link.
     */
    public String addPending(String username,
                             String hashedPassword,
                             String firstName,
                             String lastName,
                             String email) {

        String token = UUID.randomUUID().toString();

        PendingRegistration pending = new PendingRegistration(
                token, username, hashedPassword, firstName, lastName, email);

        tokenMap.put(token, pending);
        usernameMap.put(username.toLowerCase(), token);
        emailMap.put(email.toLowerCase(), token);

        return token;
    }

    // -------------------------------------------------------------------------
    // Look up + consume
    // -------------------------------------------------------------------------

    /**
     * Returns the PendingRegistration for the given token, or null if not found
     * or already consumed.
     */
    public PendingRegistration findByToken(String token) {
        return tokenMap.get(token);
    }

    /**
     * Removes the pending registration (called after it has been promoted to
     * a full Customer in MockDataStore).
     */
    public void consume(String token) {
        PendingRegistration p = tokenMap.remove(token);
        if (p != null) {
            usernameMap.remove(p.getUsername().toLowerCase());
            emailMap.remove(p.getEmail().toLowerCase());
        }
    }

    // -------------------------------------------------------------------------
    // Duplicate checks
    // -------------------------------------------------------------------------

    public boolean isUsernamePending(String username) {
        return usernameMap.containsKey(username.toLowerCase());
    }

    public boolean isEmailPending(String email) {
        return emailMap.containsKey(email.toLowerCase());
    }
}
