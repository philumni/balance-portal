package com.portal.repository;

import com.portal.data.MockDataStore;
import com.portal.model.Customer;

import java.util.Optional;

/**
 * In-memory fallback implementation of CustomerRepository.
 * Delegates to the existing MockDataStore singleton.
 * Used when MariaDB is unavailable.
 */
public class MockCustomerRepository implements CustomerRepository {

    private final MockDataStore store = MockDataStore.getInstance();

    @Override
    public Optional<Customer> findByUsername(String username) {
        // MockDataStore.authenticate requires a password — we expose find separately
        return Optional.ofNullable(store.findByUsername(username));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return Optional.ofNullable(store.findByEmail(email));
    }

    @Override
    public boolean existsByUsername(String username) {
        return store.isUsernameTaken(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.isEmailTaken(email);
    }

    @Override
    public Customer save(Customer customer) {
        return store.saveCustomer(customer);
    }
}
