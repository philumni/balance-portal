/**
 * dashboard.js
 *
 * On page load:
 *  1. Read the JWT from localStorage — redirect to login if missing
 *  2. GET /api/invoices with Authorization: Bearer <token>
 *  3. Populate the stat cards and invoice table from the JSON response
 *  4. Handle logout — DELETE token, POST /api/logout, redirect
 *
 * The server never renders HTML. This file IS the dashboard logic.
 */

const API_BASE = window.location.pathname.replace(/\/[^/]*$/, '');

// ── Page init ─────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', async () => {
  const token = localStorage.getItem('bp_token');

  // No token → go back to login immediately
  if (!token) {
    redirectToLogin();
    return;
  }

  // Populate the nav greeting from cached profile (instant, no fetch needed)
  const profile = JSON.parse(localStorage.getItem('bp_user') || '{}');
  if (profile.fullName) {
    document.getElementById('navName').textContent = profile.fullName;
    document.getElementById('pageMeta').textContent =
      `Account #${profile.accountNumber}  ·  ${profile.email}`;
  }

  // Fetch invoice data from the API
  await loadInvoices(token);

  // Hide the loading overlay
  document.getElementById('loadingOverlay').style.display = 'none';
});


// ── Fetch invoices ─────────────────────────────────────────────────────────────

async function loadInvoices(token) {
  try {
    const response = await fetch(`${API_BASE}/api/invoices`, {
      method: 'GET',
      headers: {
        // This is the key line — the JWT travels in the Authorization header,
        // NOT in a cookie. The server extracts and verifies it on every call.
        'Authorization': `Bearer ${token}`
      }
    });

    if (response.status === 401) {
      // Token expired or tampered — force re-login
      clearAuth();
      redirectToLogin();
      return;
    }

    if (!response.ok) {
      showTableError('Server error loading invoices. Please try again.');
      return;
    }

    const data = await response.json();
    renderDashboard(data);

  } catch (err) {
    showTableError('Could not reach the server. Is Tomcat running?');
    console.error('Invoice fetch error:', err);
  }
}


// ── Render ─────────────────────────────────────────────────────────────────────

function renderDashboard(data) {
  const invoices = data.invoices || [];

  // Count by status
  const count = { PAID: 0, UNPAID: 0, OVERDUE: 0, PENDING: 0 };
  invoices.forEach(inv => { if (count[inv.status] !== undefined) count[inv.status]++; });

  // Stat cards
  document.getElementById('statBalance').textContent = data.outstandingBalance || '$0.00';
  document.getElementById('statTotal').textContent   = invoices.length;
  document.getElementById('statPaid').textContent    = count.PAID;
  document.getElementById('statUnpaid').textContent  = count.UNPAID;
  document.getElementById('statOverdue').textContent = count.OVERDUE;
  document.getElementById('statPending').textContent = count.PENDING;

  // Invoice rows
  const tbody = document.getElementById('invoiceBody');

  if (invoices.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="state-msg">No invoices found.</td></tr>`;
    return;
  }

  // Build rows from JSON — no JSP, no server templates
  tbody.innerHTML = invoices.map(inv => `
    <tr data-status="${inv.status}">
      <td class="inv-num">${escHtml(inv.invoiceNumber)}</td>
      <td>${escHtml(inv.description)}</td>
      <td>${escHtml(inv.invoiceDate)}</td>
      <td>${escHtml(inv.dueDate)}</td>
      <td class="inv-amount">${escHtml(inv.formattedAmount)}</td>
      <td>
        <span class="badge ${escHtml(inv.statusCss)}">
          ${escHtml(inv.statusLabel)}
        </span>
      </td>
    </tr>
  `).join('');
}


// ── Filter (client-side — same approach as JSP version) ───────────────────────

function filterTable(status) {
  document.querySelectorAll('#invoiceBody tr[data-status]').forEach(row => {
    row.style.display =
      (status === 'ALL' || row.dataset.status === status) ? '' : 'none';
  });
}


// ── Logout ─────────────────────────────────────────────────────────────────────

async function handleLogout() {
  const token = localStorage.getItem('bp_token');

  // Tell the server (fire-and-forget — we log out regardless of response)
  // The server has nothing stateful to clean up, but the endpoint exists
  // so a future blacklist implementation doesn't need a client change.
  try {
    await fetch(`${API_BASE}/api/logout`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
  } catch (_) {
    // Ignore network errors on logout — clear local state anyway
  }

  clearAuth();
  redirectToLogin();
}


// ── Helpers ───────────────────────────────────────────────────────────────────

function clearAuth() {
  localStorage.removeItem('bp_token');
  localStorage.removeItem('bp_user');
}

function redirectToLogin() {
  window.location.href = `${API_BASE}/index.html`;
}

function showTableError(msg) {
  document.getElementById('invoiceBody').innerHTML =
    `<tr><td colspan="6" class="state-msg" style="color:#ef4444">${escHtml(msg)}</td></tr>`;
}

/**
 * Escape HTML to prevent XSS when inserting server data into the DOM.
 * Always escape untrusted strings before innerHTML injection.
 */
function escHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
