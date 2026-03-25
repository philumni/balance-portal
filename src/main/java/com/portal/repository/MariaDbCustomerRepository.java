package com.portal.repository;

import com.portal.db.ConnectionPool;
import com.portal.model.Customer;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MariaDB-backed CustomerRepository using raw JDBC.
 *
 * Every method opens a connection from HikariCP, executes its query,
 * and returns the connection to the pool via try-with-resources.
 *
 * PreparedStatements are used everywhere — never concatenate user
 * input into SQL strings (SQL injection).
 */
public class MariaDbCustomerRepository implements CustomerRepository {

    // Auto-incrementing suffix for account numbers generated here
    // In a real app, this would be a DB sequence or the AUTO_INCREMENT id
    private static final AtomicInteger counter = new AtomicInteger(2001);

    // -------------------------------------------------------------------------
    // findByUsername
    // -------------------------------------------------------------------------

    @Override
    public Optional<Customer> findByUsername(String username) {
        String sql = """
            SELECT account_number, username, password_hash,
                   first_name, last_name, email
            FROM   customers
            WHERE  LOWER(username) = LOWER(?)
            AND    verified = 1
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbCustomerRepository] findByUsername error: " + e.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // findByEmail
    // -------------------------------------------------------------------------

    @Override
    public Optional<Customer> findByEmail(String email) {
        String sql = """
            SELECT account_number, username, password_hash,
                   first_name, last_name, email
            FROM   customers
            WHERE  LOWER(email) = LOWER(?)
            AND    verified = 1
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbCustomerRepository] findByEmail error: " + e.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // existsByUsername / existsByEmail
    // -------------------------------------------------------------------------

    @Override
    public boolean existsByUsername(String username) {
        return exists("SELECT 1 FROM customers WHERE LOWER(username) = LOWER(?)", username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM customers WHERE LOWER(email) = LOWER(?)", email);
    }

    private boolean exists(String sql, String param) {
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbCustomerRepository] exists check error: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Override
    public Customer save(Customer customer) {
        // Generate account number if not already set
        String accountNumber = customer.getAccountNumber() != null
                ? customer.getAccountNumber()
                : "ACC-" + counter.getAndIncrement();

        String sql = """
            INSERT INTO customers
                (account_number, username, password_hash, first_name, last_name, email, verified)
            VALUES (?, ?, ?, ?, ?, ?, 1)
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ps.setString(2, customer.getUsername());
            ps.setString(3, customer.getHashedPassword());
            ps.setString(4, customer.getFirstName());
            ps.setString(5, customer.getLastName());
            ps.setString(6, customer.getEmail());
            ps.executeUpdate();

            System.out.println("[MariaDbCustomerRepository] Saved customer: "
                    + customer.getUsername() + " (" + accountNumber + ")");

            // Return a new Customer with the final account number
            return new Customer(accountNumber,
                    customer.getUsername(),
                    customer.getHashedPassword(),
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getEmail());

        } catch (SQLException e) {
            System.err.println("[MariaDbCustomerRepository] save error: " + e.getMessage());
            throw new RuntimeException("Failed to save customer: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Row mapper
    // -------------------------------------------------------------------------

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getString("account_number"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email")
        );
    }
}
