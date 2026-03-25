package com.portal.util;

/**
 * Holds a registration that has been submitted but not yet email-verified.
 * Updated to support both in-memory (TTL-based) and DB (expires_at column) expiry.
 */
public class PendingRegistration {

    private final String  verificationToken;
    private final String  username;
    private final String  hashedPassword;
    private final String  firstName;
    private final String  lastName;
    private final String  email;
    private final long    createdAt;
    private final boolean expired;   // set by DB repo based on expires_at column

    public static final long TTL_MS = 24 * 60 * 60 * 1000L;

    /** Constructor used by RegistrationStore (in-memory) */
    public PendingRegistration(String verificationToken, String username,
                               String hashedPassword, String firstName,
                               String lastName, String email) {
        this(verificationToken, username, hashedPassword,
             firstName, lastName, email, false);
    }

    /** Constructor used by MariaDbPendingRepository (DB-backed) */
    public PendingRegistration(String verificationToken, String username,
                               String hashedPassword, String firstName,
                               String lastName, String email,
                               boolean alreadyExpired) {
        this.verificationToken = verificationToken;
        this.username          = username;
        this.hashedPassword    = hashedPassword;
        this.firstName         = firstName;
        this.lastName          = lastName;
        this.email             = email;
        this.createdAt         = System.currentTimeMillis();
        this.expired           = alreadyExpired;
    }

    public boolean isExpired() {
        if (expired) return true;
        return (System.currentTimeMillis() - createdAt) > TTL_MS;
    }

    public String getVerificationToken() { return verificationToken; }
    public String getUsername()          { return username; }
    public String getHashedPassword()    { return hashedPassword; }
    public String getFirstName()         { return firstName; }
    public String getLastName()          { return lastName; }
    public String getEmail()             { return email; }
}
