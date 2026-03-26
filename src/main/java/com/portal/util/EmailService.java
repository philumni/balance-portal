package com.portal.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends real transactional emails via SMTP using Jakarta Mail.
 *
 * CONFIGURATION — set these environment variables before starting Tomcat:
 * -------------------------------------------------------------------------
 *   SMTP_HOST      — mail.com SMTP host         (default: smtp.mail.com)
 *   SMTP_PORT      — SMTP port                  (default: 587)
 *   SMTP_USERNAME  — your full mail.com address  e.g. yourname@mail.com
 *   SMTP_PASSWORD  — your mail.com password
 *   SMTP_FROM      — display name + address      e.g. "Balance Portal <yourname@mail.com>"
 *   APP_BASE_URL   — public root of the app      e.g. http://localhost:8080/balance-portal-reg
 *
 * HOW TO SET ENV VARS (choose one):
 * -------------------------------------------------------------------------
 * Linux/macOS (before starting Tomcat):
 *   export SMTP_USERNAME="yourname@mail.com"
 *   export SMTP_PASSWORD="yourpassword"
 *   export APP_BASE_URL="http://localhost:8080/balance-portal-reg"
 *   $CATALINA_HOME/bin/startup.sh
 *
 * Windows (cmd):
 *   set SMTP_USERNAME=yourname@mail.com
 *   set SMTP_PASSWORD=yourpassword
 *   set APP_BASE_URL=http://localhost:8080/balance-portal-reg
 *   %CATALINA_HOME%\bin\startup.bat
 *
 * IntelliJ IDEA run configuration:
 *   Run → Edit Configurations → Tomcat Server → Startup/Connection tab
 *   → Environment variables field — add all keys there.
 *
 * SWITCHING PROVIDERS:
 * -------------------------------------------------------------------------
 * Only the environment variables need to change — no code edits required.
 *
 *   Gmail:   SMTP_HOST=smtp.gmail.com   SMTP_PORT=587  (use App Password)
 *   Outlook: SMTP_HOST=smtp.office365.com SMTP_PORT=587
 *   mail.com: SMTP_HOST=smtp.mail.com   SMTP_PORT=587
 */
public class EmailService {

    // ---- Read config from environment variables at class load ----------------

    private static final String SMTP_HOST  = env("SMTP_HOST",     "smtp.mail.com");
    private static final int    SMTP_PORT  = Integer.parseInt(env("SMTP_PORT", "587"));
    private static final String USERNAME   = env("SMTP_USERNAME",  "");
    private static final String PASSWORD   = env("SMTP_PASSWORD",  "");
    private static final String FROM       = env("SMTP_FROM",      USERNAME);
    private static final String APP_BASE   = env("APP_BASE_URL",   "http://localhost:8080/balance-portal-reg");

    private EmailService() {}

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Sends a verification email to a newly registered user.
     *
     * @param toEmail    recipient email address
     * @param firstName  used in the greeting
     * @param token      UUID verification token — embedded in the link
     * @throws EmailException if the message could not be sent
     */
    public static void sendVerificationEmail(String toEmail,
                                             String firstName,
                                             String token) throws EmailException {
        validateConfig();

        String verifyUrl  = APP_BASE + "/verify.html?token=" + token;
        String subject    = "Verify your Balance Portal account";
        String htmlBody   = buildVerificationHtml(firstName, verifyUrl);
        String plainBody  = buildVerificationText(firstName, verifyUrl);

        send(toEmail, subject, htmlBody, plainBody);
    }

    // =========================================================================
    // PRIVATE — SMTP send
    // =========================================================================

    private static void send(String to,
                             String subject,
                             String htmlBody,
                             String plainBody) throws EmailException {

        Session session = buildSession();

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject, "UTF-8");

            // Multipart/alternative: email clients show HTML if they can, plain text otherwise
            Multipart multipart = new MimeMultipart("alternative");

            // Plain text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(plainBody, "UTF-8");

            // HTML part
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);
            msg.setContent(multipart);

            Transport.send(msg);

            System.out.println("[EmailService] Verification email sent to: " + to);

        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send email to " + to + ": " + e.getMessage());
            throw new EmailException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE — Session + Properties
    // =========================================================================

    /**
     * Builds a Jakarta Mail Session configured for STARTTLS on port 587.
     * Uses an Authenticator so credentials are never passed in plain text.
     */
    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");   // STARTTLS — upgrades plain → TLS
        props.put("mail.smtp.starttls.required","true");   // refuse connection if server won't upgrade
        props.put("mail.smtp.ssl.protocols",   "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "10000"); // 10s connect timeout
        props.put("mail.smtp.timeout",           "15000"); // 15s read timeout

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
    }

    // =========================================================================
    // PRIVATE — Email bodies
    // =========================================================================

    private static String buildVerificationHtml(String firstName, String verifyUrl) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
               "<body style='margin:0;padding:0;background:#0f1117;font-family:sans-serif;'>" +
               "<div style='max-width:520px;margin:40px auto;background:#1a1d27;" +
               "border:1px solid #2e3347;border-radius:12px;overflow:hidden;'>" +
               "<div style='height:4px;background:linear-gradient(90deg,transparent,#6366f1,transparent);'></div>" +
               "<div style='padding:32px 36px;'>" +
               "<div style='font-size:24px;font-weight:700;color:#e8eaf0;margin-bottom:8px;'>" +
               "⬡ BalancePortal</div>" +
               "<h1 style='color:#e8eaf0;font-size:20px;margin:24px 0 12px;'>Verify your email address</h1>" +
               "<p style='color:#9ca3af;font-size:15px;line-height:1.6;margin:0 0 28px;'>" +
               "Hi " + escHtml(firstName) + ", thanks for registering!<br>" +
               "Click the button below to activate your account. " +
               "This link expires in <strong style='color:#e8eaf0;'>24 hours</strong>.</p>" +
               "<a href='" + verifyUrl + "' style='display:inline-block;background:#6366f1;" +
               "color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;" +
               "font-weight:600;font-size:15px;'>Verify My Account →</a>" +
               "<p style='color:#6b7280;font-size:12px;margin-top:28px;line-height:1.6;'>" +
               "If the button doesn't work, copy this link into your browser:<br>" +
               "<span style='color:#818cf8;word-break:break-all;'>" + verifyUrl + "</span></p>" +
               "<hr style='border:none;border-top:1px solid #2e3347;margin:28px 0;'>" +
               "<p style='color:#4b5563;font-size:11px;margin:0;'>" +
               "If you did not create an account, ignore this email.</p>" +
               "</div></div></body></html>";
    }

    private static String buildVerificationText(String firstName, String verifyUrl) {
        return "Hi " + firstName + ",\n\n" +
               "Thanks for registering with BalancePortal!\n\n" +
               "Please verify your email address by visiting the link below.\n" +
               "This link expires in 24 hours.\n\n" +
               verifyUrl + "\n\n" +
               "If you did not create an account, ignore this email.\n\n" +
               "— The BalancePortal Team";
    }

    private static String escHtml(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // =========================================================================
    // PRIVATE — helpers
    // =========================================================================

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    /**
     * Checks that mandatory credentials are configured.
     * Fails fast with a clear error rather than a cryptic SMTP rejection.
     */
    private static void validateConfig() throws EmailException {
        if (USERNAME.isBlank() || PASSWORD.isBlank()) {
            throw new EmailException(
                "SMTP credentials not configured. " +
                "Set environment variables SMTP_USERNAME and SMTP_PASSWORD before starting Tomcat.");
        }
    }

    // =========================================================================
    // Checked exception — callers must decide how to handle send failures
    // =========================================================================

    public static class EmailException extends Exception {
        public EmailException(String message)                  { super(message); }
        public EmailException(String message, Throwable cause) { super(message, cause); }
    }
}
