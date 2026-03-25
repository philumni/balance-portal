package com.portal.servlet;

import com.portal.db.RepositoryFactory;
import com.portal.model.Customer;
import com.portal.repository.CustomerRepository;
import com.portal.repository.InvoiceRepository;
import com.portal.repository.PendingRegistrationRepository;
import com.portal.util.JwtUtil;
import com.portal.util.PendingRegistration;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import com.portal.model.Invoice;
import com.portal.model.Invoice.Status;

/**
 * GET /api/verify?token=UUID
 *
 * Verifies the email token, promotes the pending registration to a full
 * Customer, seeds sample invoices, and issues a JWT so the user is
 * immediately logged in.
 *
 * Now uses RepositoryFactory — routes to MariaDB or MockDataStore.
 */
@WebServlet("/api/verify")
public class VerifyServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCorsHeaders(resp);

        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Verification token is missing.");
            return;
        }

        PendingRegistrationRepository pending = RepositoryFactory.pending();

        Optional<PendingRegistration> opt = pending.findByToken(token);

        if (opt.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_GONE,
                    "Verification link is invalid or has already been used.");
            return;
        }

        PendingRegistration pr = opt.get();

        if (pr.isExpired()) {
            pending.delete(token);
            writeError(resp, HttpServletResponse.SC_GONE,
                    "Verification link has expired (24-hour limit). Please register again.");
            return;
        }

        // ---- Promote to full Customer ----
        CustomerRepository customers = RepositoryFactory.customers();

        Customer customer = customers.save(new Customer(
                null,                    // account number assigned by repository
                pr.getUsername(),
                pr.getHashedPassword(),
                pr.getFirstName(),
                pr.getLastName(),
                pr.getEmail()
        ));

        // ---- Seed sample invoices ----
        InvoiceRepository invoices = RepositoryFactory.invoices();
        invoices.saveAll(customer.getAccountNumber(), buildSampleInvoices(customer.getAccountNumber()));

        // ---- Consume the pending entry ----
        pending.delete(token);

        // ---- Issue JWT — user is immediately logged in ----
        String jwt = JwtUtil.generateToken(
                customer.getUsername(),
                customer.getAccountNumber(),
                customer.getFullName());

        System.out.println("[VerifyServlet] Account activated: "
                + customer.getUsername() + " / " + customer.getAccountNumber()
                + " [" + (RepositoryFactory.isUsingDb() ? "MariaDB" : "Mock") + "]");

        writeJson(resp, Map.of(
                "message",       "Email verified! Your account is ready.",
                "token",         jwt,
                "username",      customer.getUsername(),
                "fullName",      customer.getFullName(),
                "accountNumber", customer.getAccountNumber(),
                "email",         customer.getEmail()
        ));
    }

    // ---- Sample invoices for new accounts ----
    private java.util.List<Invoice> buildSampleInvoices(String accountNumber) {
        String    p   = "INV-A" + accountNumber.substring(4);
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
