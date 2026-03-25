package com.portal.data;

import com.portal.model.Customer;
import com.portal.model.Invoice;
import com.portal.model.Invoice.Status;
import com.portal.util.PasswordUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton in-memory data store — used as fallback when MariaDB is unavailable.
 *
 * Additional public methods exposed for MockCustomerRepository and MockInvoiceRepository:
 *   findByUsername()  — lookup without password check
 *   findByEmail()     — lookup by email
 *   saveCustomer()    — add a new Customer
 *   saveInvoices()    — add invoices for an account
 */
public class MockDataStore {

    private static final MockDataStore INSTANCE = new MockDataStore();
    public  static MockDataStore getInstance()  { return INSTANCE; }

    private final Map<String, Customer>      byUsername  = new ConcurrentHashMap<>();
    private final Map<String, String>        emailIndex  = new ConcurrentHashMap<>();
    private final Map<String, List<Invoice>> byAccount   = new ConcurrentHashMap<>();
    private final AtomicInteger              counter     = new AtomicInteger(2001);

    private MockDataStore() { seed(); }

    // ── Seed ──────────────────────────────────────────────────────────────────

    private void seed() {
        addCustomer(new Customer("ACC-1001","jsmith",   PasswordUtil.hash("pass123"),  "John","Smith",  "jsmith@email.com"));
        addCustomer(new Customer("ACC-1002","mjohnson", PasswordUtil.hash("secure99"), "Maria","Johnson","mjohnson@email.com"));
        addCustomer(new Customer("ACC-1003","bdavis",   PasswordUtil.hash("letmein"),  "Brian","Davis",  "bdavis@email.com"));

        byAccount.put("ACC-1001", new ArrayList<>(Arrays.asList(
            new Invoice("INV-5001","Monthly Service Fee",  LocalDate.of(2025,1,15),LocalDate.of(2025,2,15),  150.00,Status.PAID),
            new Invoice("INV-5002","Equipment Rental",     LocalDate.of(2025,2,10),LocalDate.of(2025,3,10),  320.50,Status.PAID),
            new Invoice("INV-5003","Support Package - Q1", LocalDate.of(2025,3,1), LocalDate.of(2025,4,1),   499.99,Status.OVERDUE),
            new Invoice("INV-5004","Consulting Hours",     LocalDate.of(2025,4,5), LocalDate.of(2025,5,5),   875.00,Status.UNPAID),
            new Invoice("INV-5005","License Renewal",      LocalDate.of(2025,5,20),LocalDate.of(2025,6,20), 1200.00,Status.PENDING)
        )));
        byAccount.put("ACC-1002", new ArrayList<>(Arrays.asList(
            new Invoice("INV-6001","Annual Subscription",     LocalDate.of(2025,1,1), LocalDate.of(2025,1,31), 2400.00,Status.PAID),
            new Invoice("INV-6002","Implementation Services", LocalDate.of(2025,2,14),LocalDate.of(2025,3,14), 3150.75,Status.OVERDUE),
            new Invoice("INV-6003","Training Session",        LocalDate.of(2025,3,22),LocalDate.of(2025,4,22),  600.00,Status.UNPAID),
            new Invoice("INV-6004","Hardware Maintenance",    LocalDate.of(2025,4,30),LocalDate.of(2025,5,30),  445.00,Status.PENDING)
        )));
        byAccount.put("ACC-1003", new ArrayList<>(Arrays.asList(
            new Invoice("INV-7001","Starter Plan - Jan", LocalDate.of(2025,1,5), LocalDate.of(2025,2,5),  89.99,Status.PAID),
            new Invoice("INV-7002","Starter Plan - Feb", LocalDate.of(2025,2,5), LocalDate.of(2025,3,5),  89.99,Status.PAID),
            new Invoice("INV-7003","Starter Plan - Mar", LocalDate.of(2025,3,5), LocalDate.of(2025,4,5),  89.99,Status.PAID),
            new Invoice("INV-7004","Upgrade to Pro Plan",LocalDate.of(2025,4,10),LocalDate.of(2025,5,10), 250.00,Status.UNPAID),
            new Invoice("INV-7005","Pro Plan - Apr",     LocalDate.of(2025,4,10),LocalDate.of(2025,5,10), 199.99,Status.OVERDUE),
            new Invoice("INV-7006","Pro Plan - May",     LocalDate.of(2025,5,10),LocalDate.of(2025,6,10), 199.99,Status.PENDING)
        )));
    }

    private void addCustomer(Customer c) {
        byUsername.put(c.getUsername().toLowerCase(), c);
        emailIndex.put(c.getEmail().toLowerCase(), c.getUsername().toLowerCase());
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public Customer authenticate(String username, String password) {
        if (username == null || password == null) return null;
        Customer c = byUsername.get(username.trim().toLowerCase());
        if (c == null) return null;
        return PasswordUtil.verify(password, c.getHashedPassword()) ? c : null;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public Customer findByUsername(String username) {
        return byUsername.get(username == null ? "" : username.trim().toLowerCase());
    }

    public Customer findByEmail(String email) {
        if (email == null) return null;
        String username = emailIndex.get(email.trim().toLowerCase());
        return username == null ? null : byUsername.get(username);
    }

    public boolean isUsernameTaken(String username) {
        return byUsername.containsKey(username == null ? "" : username.trim().toLowerCase());
    }

    public boolean isEmailTaken(String email) {
        return emailIndex.containsKey(email == null ? "" : email.trim().toLowerCase());
    }

    // ── Registration ─────────────────────────────────────────────────────────

    public Customer registerNewCustomer(String username, String hashedPassword,
                                        String firstName, String lastName, String email) {
        String accountNumber = "ACC-" + counter.getAndIncrement();
        Customer customer = new Customer(accountNumber, username, hashedPassword,
                                         firstName, lastName, email);
        addCustomer(customer);
        byAccount.put(accountNumber, buildSampleInvoices(accountNumber));
        return customer;
    }

    public Customer saveCustomer(Customer customer) {
        String acct = customer.getAccountNumber() != null
                ? customer.getAccountNumber()
                : "ACC-" + counter.getAndIncrement();
        Customer c = new Customer(acct, customer.getUsername(), customer.getHashedPassword(),
                                  customer.getFirstName(), customer.getLastName(), customer.getEmail());
        addCustomer(c);
        return c;
    }

    public void saveInvoices(String accountNumber, List<Invoice> invoices) {
        byAccount.put(accountNumber, new ArrayList<>(invoices));
    }

    // ── Invoices ──────────────────────────────────────────────────────────────

    public List<Invoice> getInvoices(String accountNumber) {
        return Collections.unmodifiableList(
            byAccount.getOrDefault(accountNumber, Collections.emptyList()));
    }

    public double getOutstandingBalance(String accountNumber) {
        return getInvoices(accountNumber).stream()
                .filter(Invoice::isOutstanding)
                .mapToDouble(Invoice::getAmount)
                .sum();
    }

    private List<Invoice> buildSampleInvoices(String accountNumber) {
        String p  = "INV-A" + accountNumber.substring(4);
        LocalDate now = LocalDate.now();
        return new ArrayList<>(Arrays.asList(
            new Invoice(p+"-01","Welcome Credit",       now.minusMonths(3),now.minusMonths(2),  50.00,Status.PAID),
            new Invoice(p+"-02","Monthly Service Fee",  now.minusMonths(2),now.minusMonths(1), 150.00,Status.PAID),
            new Invoice(p+"-03","Support Package",      now.minusMonths(1),now,               299.99,Status.OVERDUE),
            new Invoice(p+"-04","Platform License",     now.minusDays(15), now.plusDays(15),  499.00,Status.UNPAID),
            new Invoice(p+"-05","Consulting - Kickoff", now.minusDays(5),  now.plusDays(25),  750.00,Status.PENDING)
        ));
    }
}
