package com.portal.repository;

import com.portal.model.Customer;

import java.util.Optional;

/**
 * Contract for all customer data operations.
 *
 * Two implementations exist:
 *   MariaDbCustomerRepository  — JDBC + HikariCP (primary)
 *   MockCustomerRepository     — in-memory HashMap (fallback)
 *
 * Servlets depend only on this interface — they never import
 * either implementation directly. That's the point of the pattern.
 */
public interface CustomerRepository {

    /**
     * Find a customer by username (case-insensitive).
     * Returns empty Optional if not found or not yet verified.
     */
    Optional<Customer> findByUsername(String username);

    /**
     * Find a customer by email address (case-insensitive).
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Returns true if the username is already taken by an active account.
     */
    boolean existsByUsername(String username);

    /**
     * Returns true if the email is already registered to an active account.
     */
    boolean existsByEmail(String email);

    /**
     * Persists a new verified customer and returns the saved instance
     * (with its generated account number populated).
     */
    Customer save(Customer customer);
}
