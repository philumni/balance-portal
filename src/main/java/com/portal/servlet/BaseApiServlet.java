package com.portal.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portal.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Base class for all API servlets.
 *
 * Provides:
 *  - Shared Jackson ObjectMapper
 *  - writeJson()    — serialize any object → JSON response
 *  - writeError()   — write a { "error": "..." } JSON body with an HTTP status
 *  - extractClaims() — pull and verify the Bearer token from Authorization header
 *  - CORS headers for local development
 */
public abstract class BaseApiServlet extends HttpServlet {

    // One shared ObjectMapper — thread-safe after configuration
    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -------------------------------------------------------------------------
    // JSON response helpers
    // -------------------------------------------------------------------------

    /**
     * Serializes {@code body} to JSON and writes it to the response.
     * Sets Content-Type to application/json and status to 200.
     */
    protected void writeJson(HttpServletResponse resp, Object body)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(resp.getWriter(), body);
    }

    /**
     * Writes a JSON error object: { "error": "message" }
     */
    protected void writeError(HttpServletResponse resp, int status, String message)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(resp.getWriter(), Map.of("error", message));
    }

    // -------------------------------------------------------------------------
    // JWT extraction
    // -------------------------------------------------------------------------

    /**
     * Reads the Authorization header, strips "Bearer ", validates the JWT,
     * and returns its Claims.
     *
     * Returns null and writes a 401 response if the token is missing or invalid.
     * Callers should return immediately when null is returned.
     *
     * Expected header format:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     */
    protected Claims extractClaims(HttpServletRequest req,
                                   HttpServletResponse resp)
            throws IOException {

        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or malformed Authorization header.");
            return null;
        }

        String token = authHeader.substring(7); // strip "Bearer "

        try {
            return JwtUtil.validateToken(token);
        } catch (JwtException e) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token. Please log in again.");
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // CORS — allow the static frontend to call the API during development
    // -------------------------------------------------------------------------

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    protected void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin",  "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
