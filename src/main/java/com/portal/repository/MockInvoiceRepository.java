package com.portal.repository;

import com.portal.data.MockDataStore;
import com.portal.model.Invoice;

import java.util.List;

/**
 * In-memory fallback implementation of InvoiceRepository.
 * Delegates to MockDataStore.
 */
public class MockInvoiceRepository implements InvoiceRepository {

    private final MockDataStore store = MockDataStore.getInstance();

    @Override
    public List<Invoice> findByAccountNumber(String accountNumber) {
        return store.getInvoices(accountNumber);
    }

    @Override
    public void saveAll(String accountNumber, List<Invoice> invoices) {
        store.saveInvoices(accountNumber, invoices);
    }

    @Override
    public double getOutstandingBalance(String accountNumber) {
        return store.getOutstandingBalance(accountNumber);
    }
}
