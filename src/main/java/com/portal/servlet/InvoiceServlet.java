package com.portal.servlet;

import com.portal.db.RepositoryFactory;
import com.portal.model.Invoice;
import io.jsonwebtoken.Claims;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * GET /api/invoices
 *
 * Protected endpoint — requires Bearer token.
 * Now uses RepositoryFactory — works with MariaDB or MockDataStore transparently.
 *
 * Account number is read from the JWT claims, never from query params.
 * This prevents one user from requesting another user's invoices.
 */
@WebServlet("/api/invoices")
public class InvoiceServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCorsHeaders(resp);

        // Verify JWT — returns null + writes 401 if invalid
        Claims claims = extractClaims(req, resp);
        if (claims == null) return;

        String accountNumber = (String) claims.get("accountNumber");

        List<Invoice> invoices = RepositoryFactory.invoices()
                                                  .findByAccountNumber(accountNumber);

        double balance   = RepositoryFactory.invoices()
                                            .getOutstandingBalance(accountNumber);

        String formatted = String.format("$%,.2f", balance);

        writeJson(resp, Map.of(
                "accountNumber",      accountNumber,
                "outstandingBalance", formatted,
                "invoices",           invoices,
                "dataMode",           RepositoryFactory.isUsingDb() ? "MariaDB" : "MockDataStore"
        ));
    }
}
