/**
 * register.js
 *
 * Handles the registration form:
 *  1. Client-side validation (passwords match, strength check)
 *  2. POST /api/register with JSON body
 *  3. On success — shows a "check your inbox" message
 *     The real verification email is sent server-side via SMTP.
 */

const API_BASE = window.location.pathname.replace(/\/[^/]*$/, '');

// ── Password strength meter ──────────────────────────────────────────────────

function updateStrength(password) {
  const fill  = document.getElementById('strengthFill');
  const label = document.getElementById('strengthLabel');

  let score = 0;
  if (password.length >= 8)             score++;
  if (password.match(/[A-Z]/))          score++;
  if (password.match(/[0-9]/))          score++;
  if (password.match(/[^A-Za-z0-9]/))  score++;
  if (password.length >= 12)            score++;

  const levels = [
    { pct: '0%',   color: 'transparent', text: '' },
    { pct: '25%',  color: '#ef4444',     text: 'Weak' },
    { pct: '50%',  color: '#f97316',     text: 'Fair' },
    { pct: '75%',  color: '#eab308',     text: 'Good' },
    { pct: '90%',  color: '#22c55e',     text: 'Strong' },
    { pct: '100%', color: '#10b981',     text: 'Very strong' },
  ];

  const level = levels[Math.min(score, 5)];
  fill.style.width      = level.pct;
  fill.style.background = level.color;
  label.textContent     = level.text;
  label.style.color     = level.color;
}

// ── Main registration handler ─────────────────────────────────────────────────

async function handleRegister() {
  const firstName = document.getElementById('firstName').value.trim();
  const lastName  = document.getElementById('lastName').value.trim();
  const email     = document.getElementById('email').value.trim();
  const username  = document.getElementById('username').value.trim();
  const password  = document.getElementById('password').value;
  const confirm   = document.getElementById('confirm').value;

  hideAlert();

  // ---- Client-side validation ----
  if (!firstName || !lastName || !email || !username || !password || !confirm) {
    showAlert('error', 'All fields are required.');
    return;
  }

  if (password !== confirm) {
    showAlert('error', 'Passwords do not match.');
    document.getElementById('confirm').classList.add('invalid');
    return;
  }

  document.getElementById('confirm').classList.remove('invalid');

  if (password.length < 8 || !password.match(/[A-Z]/) || !password.match(/[0-9]/)) {
    showAlert('error',
      'Password must be at least 8 characters with one uppercase letter and one number.');
    return;
  }

  setLoading(true);

  try {
    const response = await fetch(`${API_BASE}/api/register`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ username, password, firstName, lastName, email })
    });

    const data = await response.json();

    if (!response.ok) {
      showAlert('error', data.error || 'Registration failed. Please try again.');
      return;
    }

    // ---- Success — real email has been sent ----
    showAlert('success', data.message);

    // Disable form — prevents double-submission
    document.getElementById('registerBtn').disabled = true;
    document.querySelectorAll('input').forEach(el => el.disabled = true);

  } catch (err) {
    showAlert('error', 'Could not reach the server. Is Tomcat running?');
    console.error('Register error:', err);
  } finally {
    setLoading(false);
  }
}

// ── UI helpers ────────────────────────────────────────────────────────────────

function setLoading(on) {
  document.getElementById('registerBtn').disabled     = on;
  document.getElementById('btnText').style.display    = on ? 'none'  : 'inline';
  document.getElementById('btnSpinner').style.display = on ? 'block' : 'none';
}

function showAlert(type, message) {
  const el = document.getElementById('alert');
  el.className   = `alert alert-${type} show`;
  el.textContent = message;
}

function hideAlert() {
  document.getElementById('alert').classList.remove('show');
}

document.addEventListener('keydown', e => {
  if (e.key === 'Enter') handleRegister();
});
