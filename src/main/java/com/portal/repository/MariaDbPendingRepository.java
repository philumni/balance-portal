package com.portal.repository;

import com.portal.db.ConnectionPool;
import com.portal.util.PendingRegistration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * MariaDB-backed PendingRegistrationRepository.
 *
 * Stores pending registrations in the pending_registrations table.
 * Unlike the in-memory RegistrationStore, these survive a Tomcat restart —
 * a user who registered but hasn't verified yet won't lose their token
 * if the server is restarted within the 24-hour window.
 */
public class MariaDbPendingRepository implements PendingRegistrationRepository {

    @Override
    public String save(String username, String hashedPassword,
                       String firstName, String lastName, String email) {

        String token     = UUID.randomUUID().toString();
        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime expires = now.plusHours(24);

        String sql = """
            INSERT INTO pending_registrations
                (token, username, password_hash, first_name, last_name, email, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            ps.setString(2, username);
            ps.setString(3, hashedPassword);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.setString(6, email);
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.setTimestamp(8, Timestamp.valueOf(expires));
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[MariaDbPendingRepository] save error: " + e.getMessage());
            throw new RuntimeException("Failed to save pending registration: " + e.getMessage(), e);
        }
        return token;
    }

    @Override
    public Optional<PendingRegistration> findByToken(String token) {
        String sql = """
            SELECT token, username, password_hash, first_name, last_name, email, expires_at
            FROM   pending_registrations
            WHERE  token = ?
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp expires = rs.getTimestamp("expires_at");
                    boolean expired = expires != null
                            && expires.toLocalDateTime().isBefore(LocalDateTime.now());

                    PendingRegistration pr = new PendingRegistration(
                            rs.getString("token"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            expired
                    );
                    return Optional.of(pr);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbPendingRepository] findByToken error: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void delete(String token) {
        String sql = "DELETE FROM pending_registrations WHERE token = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MariaDbPendingRepository] delete error: " + e.getMessage());
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return exists("SELECT 1 FROM pending_registrations WHERE LOWER(username) = LOWER(?)", username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM pending_registrations WHERE LOWER(email) = LOWER(?)", email);
    }

    private boolean exists(String sql, String param) {
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbPendingRepository] exists error: " + e.getMessage());
            return false;
        }
    }
}
