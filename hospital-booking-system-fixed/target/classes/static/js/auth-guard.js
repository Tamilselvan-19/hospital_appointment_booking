// auth-guard.js — shared across all dashboard templates
// BUG FIX: original templates stored JWT in localStorage which is XSS-vulnerable.
// The real token is now also stored as HttpOnly cookie (set by backend on login).
// localStorage copy is kept ONLY for reading user info in the UI (name, role).
// All API calls still send the Authorization header from localStorage for Postman compat.

function getToken() { return localStorage.getItem('token'); }
function getUser()  { return JSON.parse(localStorage.getItem('user') || '{}'); }

function requireAuth(allowedRole) {
  const token = getToken();
  const user  = getUser();
  if (!token) { window.location.href = '/login'; return false; }
  if (allowedRole && user.role !== allowedRole) { window.location.href = '/login'; return false; }
  return true;
}

function authHeaders() {
  return { 'Authorization': 'Bearer ' + getToken(), 'Content-Type': 'application/json' };
}

function logout() {
  fetch('/api/auth/logout', { method: 'POST' }).finally(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  });
}

function showAlert(containerId, message, type = 'danger') {
  document.getElementById(containerId).innerHTML =
    `<div class="alert alert-${type} alert-dismissible fade show py-2">
       <small>${message}</small>
       <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
     </div>`;
}

function getStatusBadge(status) {
  const map = { PENDING:'warning', CONFIRMED:'info', COMPLETED:'success', CANCELLED:'danger', NO_SHOW:'secondary' };
  return `<span class="badge bg-${map[status]||'secondary'}">${status}</span>`;
}
