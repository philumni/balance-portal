package com.portal.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * POST /api/logout
 *
 * With JWT, logout is stateless — the server has nothing to destroy.
 * The client is responsible for deleting the token from localStorage.
 *
 * This endpoint exists so the client has a clean logout flow and so
 * future implementations can add a token blacklist (e.g. Redis) here
 * without changing the client contract.
 *
 * Response (200):
 *   { "message": "Logged out successfully." }
 */
@WebServlet("/api/logout")
public class LogoutServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCorsHeaders(resp);

        // With JWT, the server has no session to invalidate.
        // The client will delete the token from localStorage on receipt of this response.
        // A production system would add the token's JTI to a Redis blacklist here.

        writeJson(resp, Map.of("message", "Logged out successfully."));
    }
}
