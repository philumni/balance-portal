package com.portal.repository;

import com.portal.db.ConnectionPool;
import com.portal.model.Invoice;
import com.portal.model.Invoice.Status;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MariaDB-backed InvoiceRepository using raw JDBC.
 *
 * Notable: getOutstandingBalance() does the SUM in SQL rather than
 * loading all invoices into Java memory first. For large accounts
 * with thousands of invoices this is significantly more efficient.
 */
public class MariaDbInvoiceRepository implements InvoiceRepository {

    // -------------------------------------------------------------------------
    // findByAccountNumber
    // -------------------------------------------------------------------------

    @Override
    public List<Invoice> findByAccountNumber(String accountNumber) {
        String sql = """
            SELECT invoice_number, description, invoice_date,
                   due_date, amount, status
            FROM   invoices
            WHERE  account_number = ?
            ORDER  BY invoice_date DESC
            """;

        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbInvoiceRepository] findByAccountNumber error: "
                    + e.getMessage());
        }
        return invoices;
    }

    // -------------------------------------------------------------------------
    // saveAll — used when a new customer is created
    // -------------------------------------------------------------------------

    @Override
    public void saveAll(String accountNumber, List<Invoice> invoices) {
        String sql = """
            INSERT INTO invoices
                (invoice_number, account_number, description,
                 invoice_date, due_date, amount, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        // Use a single connection + batch for efficiency
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);  // wrap the whole batch in one transaction

            for (Invoice inv : invoices) {
                ps.setString(1, inv.getInvoiceNumber());
                ps.setString(2, accountNumber);
                ps.setString(3, inv.getDescription());
                ps.setDate(4, Date.valueOf(inv.getInvoiceDate()));
                ps.setDate(5, Date.valueOf(inv.getDueDate()));
                ps.setDouble(6, inv.getAmount());
                ps.setString(7, inv.getStatus().name());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

            System.out.println("[MariaDbInvoiceRepository] Saved "
                    + invoices.size() + " invoices for " + accountNumber);

        } catch (SQLException e) {
            System.err.println("[MariaDbInvoiceRepository] saveAll error: " + e.getMessage());
            throw new RuntimeException("Failed to save invoices: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // getOutstandingBalance — aggregated in SQL
    // -------------------------------------------------------------------------

    @Override
    public double getOutstandingBalance(String accountNumber) {
        String sql = """
            SELECT COALESCE(SUM(amount), 0)
            FROM   invoices
            WHERE  account_number = ?
            AND    status IN ('UNPAID', 'OVERDUE', 'PENDING')
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("[MariaDbInvoiceRepository] getOutstandingBalance error: "
                    + e.getMessage());
        }
        return 0.0;
    }

    // -------------------------------------------------------------------------
    // Row mapper
    // -------------------------------------------------------------------------

    private Invoice mapRow(ResultSet rs) throws SQLException {
        LocalDate invoiceDate = rs.getDate("invoice_date").toLocalDate();
        LocalDate dueDate     = rs.getDate("due_date").toLocalDate();
        Status    status      = Status.valueOf(rs.getString("status"));

        return new Invoice(
                rs.getString("invoice_number"),
                rs.getString("description"),
                invoiceDate,
                dueDate,
                rs.getDouble("amount"),
                status
        );
    }
}
