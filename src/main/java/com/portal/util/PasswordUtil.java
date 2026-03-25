package com.portal.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility for hashing and verifying passwords using BCrypt.
 *
 * WHY BCRYPT?
 * -----------
 * Plain SHA/MD5 hashes are fast — that's the problem. An attacker with
 * a stolen database can try billions of guesses per second.
 * BCrypt is intentionally slow (cost factor) and includes a salt
 * automatically, making brute-force and rainbow-table attacks impractical.
 *
 * COST FACTOR
 * -----------
 * Cost 12 means 2^12 = 4096 iterations. On modern hardware this takes
 * ~250ms per hash — barely noticeable to a user logging in once,
 * but brutal for an attacker trying millions of passwords.
 * Increase cost as hardware gets faster over time.
 */
public class PasswordUtil {

    private static final int COST = 12;

    private PasswordUtil() {}

    /**
     * Hashes a plain-text password.
     * The returned string contains the salt and cost factor — store the
     * whole thing. Never store the plain-text password.
     *
     * @param plainText the password the user typed
     * @return BCrypt hash string, e.g. "$2a$12$..."
     */
    public static String hash(String plainText) {
        return BCrypt.withDefaults()
                     .hashToString(COST, plainText.toCharArray());
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param plainText   the password the user just typed
     * @param storedHash  the hash stored in the database
     * @return true if the password matches
     */
    public static boolean verify(String plainText, String storedHash) {
        BCrypt.Result result = BCrypt.verifyer()
                                     .verify(plainText.toCharArray(), storedHash);
        return result.verified;
    }
}
