# SMTP Email Setup Guide

This app sends real verification emails using Jakarta Mail over SMTP.
No mocks. No console output. A real email lands in the user's inbox.

---

## mail.com Settings

| Setting       | Value                  |
|---------------|------------------------|
| SMTP Server   | `smtp.mail.com`        |
| Port          | `587`                  |
| Encryption    | STARTTLS               |
| Auth required | Yes                    |
| Username      | Your full mail.com address (e.g. `yourname@mail.com`) |
| Password      | Your mail.com password |

> **Important:** mail.com free accounts may block SMTP access from third-party apps.
> If you get an authentication error, log into mail.com web → Settings → POP3 & IMAP
> and enable external access. If it remains blocked, the same code works identically
> with Gmail (use an App Password) or any other SMTP provider — just change the env vars.

---

## Step 1 — Set Environment Variables

The app reads credentials from environment variables.
**Never hard-code credentials in source files.**

### macOS / Linux

```bash
export SMTP_HOST="smtp.mail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="yourname@mail.com"
export SMTP_PASSWORD="yourpassword"
export SMTP_FROM="Balance Portal <yourname@mail.com>"
export APP_BASE_URL="http://localhost:8080/balance-portal-reg"
```

Add these lines to `~/.zshrc` or `~/.bashrc` to persist them across sessions,
then run `source ~/.zshrc`.

### Windows (Command Prompt)

```cmd
set SMTP_HOST=smtp.mail.com
set SMTP_PORT=587
set SMTP_USERNAME=yourname@mail.com
set SMTP_PASSWORD=yourpassword
set SMTP_FROM=Balance Portal <yourname@mail.com>
set APP_BASE_URL=http://localhost:8080/balance-portal-reg
```

### Windows (PowerShell)

```powershell
$env:SMTP_HOST     = "smtp.mail.com"
$env:SMTP_PORT     = "587"
$env:SMTP_USERNAME = "yourname@mail.com"
$env:SMTP_PASSWORD = "yourpassword"
$env:SMTP_FROM     = "Balance Portal <yourname@mail.com>"
$env:APP_BASE_URL  = "http://localhost:8080/balance-portal-reg"
```

---

## Step 2 — Set Variables in IntelliJ (Recommended for Development)

1. Open **Run → Edit Configurations**
2. Select your **Tomcat Server** configuration
3. Click the **Startup/Connection** tab
4. Click the **Environment variables** field (the folder icon)
5. Add each key-value pair from Step 1

This way you never have to set them system-wide and they don't appear in your shell history.

---

## Step 3 — Build and Deploy

```bash
mvn clean package
cp target/balance-portal-reg.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

---

## Step 4 — Test It

1. Open `http://localhost:8080/balance-portal-reg/`
2. Click **Create an Account**
3. Fill out the form — use a real email address you can check
4. Click **Create Account**
5. Check your inbox for the verification email (check spam too)
6. Click the link — you'll be logged in automatically and redirected to the dashboard

---

## Switching Email Providers

Only the environment variables change. The code is identical.

### Gmail (App Password required)

Google blocks plain passwords for SMTP. You must use an App Password:
1. Google Account → Security → 2-Step Verification → App passwords
2. Generate a password for "Mail" + "Other device"

```bash
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="yourname@gmail.com"
export SMTP_PASSWORD="xxxx xxxx xxxx xxxx"   # 16-char App Password
export SMTP_FROM="Balance Portal <yourname@gmail.com>"
```

### Outlook / Office 365

```bash
export SMTP_HOST="smtp.office365.com"
export SMTP_PORT="587"
export SMTP_USERNAME="yourname@outlook.com"
export SMTP_PASSWORD="yourpassword"
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `AuthenticationFailedException` | Wrong credentials or SMTP not enabled | Verify username/password; enable IMAP/SMTP in mail.com settings |
| `ConnectException: Connection refused` | Port blocked by firewall | Try port 465 with `mail.smtp.ssl.enable=true` |
| `MessagingException: Could not connect` | SMTP_HOST wrong or network issue | Ping smtp.mail.com; check env vars loaded |
| Email lands in spam | No SPF/DKIM on sending domain | Normal for dev; add DNS records in production |
| `SMTP credentials not configured` | Env vars not set before Tomcat started | Set vars in the **same shell** that starts Tomcat, or use IntelliJ env var field |
