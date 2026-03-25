/**
 * verify.js
 *
 * Runs on verify.html.
 * Reads the ?token= parameter from the URL, calls GET /api/verify,
 * and on success stores the JWT (just like login.js does) then
 * redirects to the dashboard.
 *
 * This means:  register → verify → dashboard  with no separate login step.
 * The server issues a JWT in the verify response, so the user is
 * immediately authenticated the moment their email is confirmed.
 */

const API_BASE = '/balance-portal-reg';

(async function run() {
  const params = new URLSearchParams(window.location.search);
  const token  = params.get('token');

  if (!token) {
    showError('No verification token found in the URL.');
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/api/verify?token=${encodeURIComponent(token)}`);
    const data     = await response.json();

    if (!response.ok) {
      showError(data.error || 'Verification failed.');
      return;
    }

    // ---- Store JWT + profile (same shape as login.js) ----
    localStorage.setItem('bp_token', data.token);
    localStorage.setItem('bp_user',  JSON.stringify({
      username:      data.username,
      fullName:      data.fullName,
      accountNumber: data.accountNumber,
      email:         data.email
    }));

    // ---- Show success UI ----
    document.getElementById('statusIcon').textContent  = '✅';
    document.getElementById('statusTitle').textContent = 'Email Verified!';
    document.getElementById('statusMsg').textContent   =
      `Welcome, ${data.fullName}! Your account (${data.accountNumber}) is ready.`;
    document.getElementById('spinnerWrap').style.display = 'none';
    document.getElementById('dashBtn').classList.add('show');

    // Auto-redirect after 2 seconds
    setTimeout(() => {
      window.location.href = `${API_BASE}/dashboard.html`;
    }, 2000);

  } catch (err) {
    showError('Could not reach the server. Is Tomcat running?');
    console.error('Verify error:', err);
  }
})();


function showError(message) {
  document.getElementById('statusIcon').textContent  = '❌';
  document.getElementById('statusTitle').textContent = 'Verification Failed';
  document.getElementById('statusMsg').textContent   = '';
  document.getElementById('spinnerWrap').style.display = 'none';

  const alert = document.getElementById('alert');
  alert.className   = 'alert alert-error show';
  alert.textContent = message;

  // Give them a way back
  const btn = document.getElementById('dashBtn');
  btn.textContent = '← Back to Login';
  btn.href        = `${API_BASE}/index.html`;
  btn.classList.add('show');
}
