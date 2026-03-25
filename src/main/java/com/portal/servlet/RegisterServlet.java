package com.portal.servlet;

import com.portal.db.RepositoryFactory;
import com.portal.repository.CustomerRepository;
import com.portal.repository.PendingRegistrationRepository;
import com.portal.util.EmailService;
import com.portal.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * POST /api/register
 *
 * Now uses RepositoryFactory — routes to MariaDB or MockDataStore transparently.
 *
 * When MariaDB is active, pending registrations are written to the
 * pending_registrations table and survive a Tomcat restart.
 * When falling back to Mock, they live in RegistrationStore in memory.
 */
@WebServlet("/api/register")
public class RegisterServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCorsHeaders(resp);

        Map<?, ?> body;
        try {
            body = MAPPER.readValue(req.getInputStream(), Map.class);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Request body must be valid JSON.");
            return;
        }

        String username  = trim(body, "username");
        String password  = trim(body, "password");
        String firstName = trim(body, "firstName");
        String lastName  = trim(body, "lastName");
        String email     = trim(body, "email");

        // ---- Presence ----
        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty()
                || lastName.isEmpty() || email.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "All fields are required: username, password, firstName, lastName, email.");
            return;
        }

        // ---- Username format ----
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Username must be 3–20 characters: letters, numbers, or underscores only.");
            return;
        }

        // ---- Password strength ----
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[0-9].*")) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Password must be at least 8 characters with one uppercase letter and one number.");
            return;
        }

        // ---- Email format ----
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Please enter a valid email address.");
            return;
        }

        CustomerRepository            customers = RepositoryFactory.customers();
        PendingRegistrationRepository pending   = RepositoryFactory.pending();

        // ---- Duplicate checks across both active accounts and pending ----
        if (customers.existsByUsername(username) || pending.existsByUsername(username)) {
            writeError(resp, HttpServletResponse.SC_CONFLICT,
                    "Username '" + username + "' is already taken.");
            return;
        }

        if (customers.existsByEmail(email) || pending.existsByEmail(email)) {
            writeError(resp, HttpServletResponse.SC_CONFLICT,
                    "An account with that email address already exists.");
            return;
        }

        // ---- Hash + queue ----
        String hashed = PasswordUtil.hash(password);
        String token  = pending.save(username, hashed, firstName, lastName, email);

        // ---- Send real verification email ----
        try {
            EmailService.sendVerificationEmail(email, firstName, token);
        } catch (EmailService.EmailException e) {
            // Roll back the pending entry so the user can try again cleanly
            pending.delete(token);
            System.err.println("[RegisterServlet] Email send failed: " + e.getMessage());
            writeError(resp, 502,
                    "Your account was created but we could not send the verification email. " +
                    "Please check your address and try again. Detail: " + e.getMessage());
            return;
        }

        writeJson(resp, Map.of(
                "message", "Verification email sent to " + email + ". " +
                           "Check your inbox (and spam folder) and click the link to activate."
        ));
    }

    private String trim(Map<?, ?> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : v.toString().trim();
    }
}
