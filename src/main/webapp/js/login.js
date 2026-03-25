/**
 * login.js
 *
 * Handles the login form:
 *  1. POSTs credentials as JSON to /api/login
 *  2. On success — stores the JWT in localStorage, redirects to dashboard
 *  3. On failure — shows the error message from the server
 *
 * Why fetch() instead of XMLHttpRequest?
 *   fetch() is the modern Ajax API — cleaner, Promise-based, no callback hell.
 *   It is fully supported in all current browsers.
 */

// Context path — change this if you deploy under a different root
const API_BASE = '/balance-portal-jwt';

// ── On page load: if a valid token already exists, skip straight to dashboard ──
(function checkExistingToken() {
  const token = localStorage.getItem('bp_token');
  if (token) {
    window.location.href = `${API_BASE}/dashboard.html`;
  }
})();


// ── Click-to-fill demo rows ──
function fill(username, password) {
  document.getElementById('username').value = username;
  document.getElementById('password').value = password;
  document.getElementById('username').focus();
}


// ── Main login handler ──
async function handleLogin() {
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;

  // Basic client-side guard
  if (!username || !password) {
    showAlert('error', 'Please enter both username and password.');
    return;
  }

  setLoading(true);
  hideAlert();

  try {
    const response = await fetch(`${API_BASE}/api/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      // Serialize credentials as JSON — the servlet reads this with Jackson
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();

    if (!response.ok) {
      // Server returned 401 or 400 — show the error message from the JSON body
      showAlert('error', data.error || 'Login failed. Please try again.');
      return;
    }

    // ── Success ──
    // Store the JWT. The dashboard will read this on every page load
    // and send it in the Authorization header for every API call.
    localStorage.setItem('bp_token',   data.token);
    localStorage.setItem('bp_user',    JSON.stringify({
      username:      data.username,
      fullName:      data.fullName,
      accountNumber: data.accountNumber,
      email:         data.email
    }));

    showAlert('success', `Welcome back, ${data.fullName}! Redirecting…`);

    setTimeout(() => {
      window.location.href = `${API_BASE}/dashboard.html`;
    }, 600);

  } catch (err) {
    // Network error or JSON parse failure
    showAlert('error', 'Could not reach the server. Is Tomcat running?');
    console.error('Login error:', err);
  } finally {
    setLoading(false);
  }
}

// Allow Enter key to submit
document.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') handleLogin();
});


// ── UI helpers ──

function setLoading(on) {
  const btn     = document.getElementById('loginBtn');
  const text    = document.getElementById('btnText');
  const spinner = document.getElementById('btnSpinner');

  btn.disabled        = on;
  text.style.display  = on ? 'none'  : 'inline';
  spinner.style.display = on ? 'block' : 'none';
}

function showAlert(type, message) {
  const el = document.getElementById('alert');
  el.className  = `alert alert-${type} show`;
  el.textContent = message;
}

function hideAlert() {
  document.getElementById('alert').classList.remove('show');
}
