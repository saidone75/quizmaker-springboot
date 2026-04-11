/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// ===== SCREEN NAVIGATION =====
function goTo(screenId) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    const target = document.getElementById('screen-' + screenId);
    if (target) target.classList.add('active');
}

// ===== THEME =====
function resolveInitialTheme() {
    const profilePreference = document.querySelector('meta[name="quizmaker-theme-preference"]')?.content || '';
    if (profilePreference === 'light' || profilePreference === 'dark' || profilePreference === 'zenburn' || profilePreference === 'true-summer') {
        return profilePreference;
    }
    return 'light';
}

function applyTheme(theme) {
    const nextTheme = theme === 'dark' || theme === 'zenburn' || theme === 'true-summer' ? theme : 'light';
    document.documentElement.dataset.theme = nextTheme;
    document.body.dataset.theme = nextTheme;

    const toggle = document.getElementById('theme-toggle');
    if (!toggle) return;

    const icon = toggle.querySelector('.theme-toggle-icon');
    const label = toggle.querySelector('.theme-toggle-label');
    const themeToggleContent = {
        dark: {icon: '☀️', label: 'Chiaro', ariaLabel: 'Attiva tema chiaro'},
        zenburn: {icon: '🌗', label: 'Scuro', ariaLabel: 'Attiva tema scuro'},
        default: {icon: '🌙', label: 'Scuro', ariaLabel: 'Attiva tema scuro'}
    };
    const content = themeToggleContent[nextTheme] || themeToggleContent.default;

    if (icon) icon.textContent = content.icon;
    if (label) label.textContent = content.label;
    toggle.setAttribute('aria-label', content.ariaLabel);
}

function setupThemeToggle() {
    applyTheme(resolveInitialTheme());
    const toggle = document.getElementById('theme-toggle');
    if (!toggle) return;
    toggle.addEventListener('click', () => {
        const currentTheme = document.body.dataset.theme || 'light';
        applyTheme(currentTheme === 'dark' ? 'light' : 'dark');
    });
}

document.addEventListener('DOMContentLoaded', setupThemeToggle);

// ===== LOADING OVERLAY =====
function showLoading(msg) {
    let el = document.getElementById('loading-overlay');
    if (!el) {
        el = document.createElement('div');
        el.id = 'loading-overlay';
        el.innerHTML = '<div class="loading-icon">⏳</div><p></p>';
        document.body.appendChild(el);
    }
    el.querySelector('p').textContent = msg || 'Caricamento...';
    el.style.display = 'flex';
}

function hideLoading() {
    const el = document.getElementById('loading-overlay');
    if (el) el.style.display = 'none';
}

// ===== TOAST =====
function showToast(msg) {
    let t = document.getElementById('toast');
    if (!t) {
        t = document.createElement('div');
        t.id = 'toast';
        document.body.appendChild(t);
    }
    t.textContent = '✅ ' + msg;
    t.style.opacity = '1';
    setTimeout(() => { t.style.opacity = '0'; }, 2500);
}

// ===== HTML ESCAPE =====
function escHtml(str) {
    return (str || '')
        .replaceAll('&', '&amp;')
        .replaceAll('"', '&quot;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
}

// ===== API HELPERS =====
function getCsrfHeaders(baseHeaders) {
    const headers = {...(baseHeaders || {})};
    const csrfToken = document.querySelector('meta[name="quizmaker-csrf-token"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="quizmaker-csrf-header"]')?.content || '';
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

async function apiFetch(url, options) {
    const opts = {...(options || {})};
    opts.headers = getCsrfHeaders(opts.headers);
    return fetch(url, opts);
}
