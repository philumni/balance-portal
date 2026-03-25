package com.portal.repository;

import com.portal.util.PendingRegistration;

import java.util.Optional;

/**
 * Contract for pending (unverified) registration operations.
 *
 * Two implementations:
 *   MariaDbPendingRepository  — persists to pending_registrations table
 *   MockPendingRepository     — wraps existing RegistrationStore
 *
 * Storing pending registrations in the DB means they survive a
 * Tomcat restart — unlike the in-memory RegistrationStore.
 */
public interface PendingRegistrationRepository {

    /**
     * Stores a pending registration and returns the verification token.
     */
    String save(String username, String hashedPassword,
                String firstName, String lastName, String email);

    /**
     * Looks up a pending registration by its token.
     */
    Optional<PendingRegistration> findByToken(String token);

    /**
     * Removes a pending registration after it has been verified
     * or after it has expired.
     */
    void delete(String token);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
