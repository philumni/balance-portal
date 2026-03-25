package com.portal.repository;

import com.portal.util.PendingRegistration;
import com.portal.util.RegistrationStore;

import java.util.Optional;

/**
 * In-memory fallback implementation of PendingRegistrationRepository.
 * Delegates to the existing RegistrationStore singleton.
 */
public class MockPendingRepository implements PendingRegistrationRepository {

    private final RegistrationStore store = RegistrationStore.getInstance();

    @Override
    public String save(String username, String hashedPassword,
                       String firstName, String lastName, String email) {
        return store.addPending(username, hashedPassword, firstName, lastName, email);
    }

    @Override
    public Optional<PendingRegistration> findByToken(String token) {
        return Optional.ofNullable(store.findByToken(token));
    }

    @Override
    public void delete(String token) {
        store.consume(token);
    }

    @Override
    public boolean existsByUsername(String username) {
        return store.isUsernamePending(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.isEmailPending(email);
    }
}
