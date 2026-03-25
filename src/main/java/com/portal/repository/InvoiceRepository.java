package com.portal.repository;

import com.portal.model.Invoice;

import java.util.List;

/**
 * Contract for all invoice data operations.
 *
 * Two implementations:
 *   MariaDbInvoiceRepository  — JDBC + HikariCP (primary)
 *   MockInvoiceRepository     — in-memory HashMap (fallback)
 */
public interface InvoiceRepository {

    /**
     * Returns all invoices for the given account number,
     * ordered by invoice_date descending (newest first).
     */
    List<Invoice> findByAccountNumber(String accountNumber);

    /**
     * Persists a batch of invoices for a new account.
     * Used when a new customer is registered.
     */
    void saveAll(String accountNumber, List<Invoice> invoices);

    /**
     * Returns the total outstanding balance (UNPAID + OVERDUE + PENDING)
     * for the given account number.
     * Calculated in SQL for efficiency rather than streaming in Java.
     */
    double getOutstandingBalance(String accountNumber);
}
