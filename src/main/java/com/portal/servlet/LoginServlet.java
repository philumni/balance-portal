package com.portal.servlet;

import com.portal.db.RepositoryFactory;
import com.portal.model.Customer;
import com.portal.util.JwtUtil;
import com.portal.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * POST /api/login
 * Now uses RepositoryFactory — works with MariaDB or MockDataStore transparently.
 */
@WebServlet("/api/login")
public class LoginServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCorsHeaders(resp);

        Map<?, ?> body;
        try {
            body = MAPPER.readValue(req.getInputStream(), Map.class);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body must be valid JSON.");
            return;
        }

        String username = trim(body, "username");
        String password = trim(body, "password");

        if (username.isEmpty() || password.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Find customer then verify password with BCrypt
        Optional<Customer> found = RepositoryFactory.customers().findByUsername(username);

        if (found.isEmpty() || !PasswordUtil.verify(password, found.get().getHashedPassword())) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            return;
        }

        Customer customer = found.get();
        String   token    = JwtUtil.generateToken(
                customer.getUsername(),
                customer.getAccountNumber(),
                customer.getFullName());

        writeJson(resp, Map.of(
                "token",         token,
                "username",      customer.getUsername(),
                "fullName",      customer.getFullName(),
                "accountNumber", customer.getAccountNumber(),
                "email",         customer.getEmail()
        ));
    }

    private String trim(Map<?, ?> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : v.toString().trim();
    }
}
