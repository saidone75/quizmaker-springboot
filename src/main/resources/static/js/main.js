/*
 * QuizMaker - fun quizzes for curious minds
 * Copyright (C) 2026 Saidone
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
const QUIZMAKER_THEME_KEY = 'quizmaker-theme';

function resolveInitialTheme() {
    const profilePreference = document.querySelector('meta[name="quizmaker-theme-preference"]')?.content || '';
    if (profilePreference === 'light' || profilePreference === 'dark' || profilePreference === 'zenburn') {
        return profilePreference;
    }

    const savedTheme = localStorage.getItem(QUIZMAKER_THEME_KEY);
    if (savedTheme === 'light' || savedTheme === 'dark' || savedTheme === 'zenburn') return savedTheme;
    return 'light';
}

function applyTheme(theme) {
    const nextTheme = theme === 'dark' || theme === 'zenburn' ? theme : 'light';
    document.body.setAttribute('data-theme', nextTheme);
    localStorage.setItem(QUIZMAKER_THEME_KEY, nextTheme);

    const toggle = document.getElementById('theme-toggle');
    if (!toggle) return;

    const icon = toggle.querySelector('.theme-toggle-icon');
    const label = toggle.querySelector('.theme-toggle-label');
    if (nextTheme === 'dark') {
        if (icon) icon.textContent = '☀️';
        if (label) label.textContent = 'Chiaro';
        toggle.setAttribute('aria-label', 'Attiva tema chiaro');
    } else if (nextTheme === 'zenburn') {
        if (icon) icon.textContent = '🌗';
        if (label) label.textContent = 'Scuro';
        toggle.setAttribute('aria-label', 'Attiva tema scuro');
    } else {
        if (icon) icon.textContent = '🌙';
        if (label) label.textContent = 'Scuro';
        toggle.setAttribute('aria-label', 'Attiva tema scuro');
    }
}

function setupThemeToggle() {
    applyTheme(resolveInitialTheme());
    const toggle = document.getElementById('theme-toggle');
    if (!toggle) return;
    toggle.addEventListener('click', () => {
        const currentTheme = document.body.getAttribute('data-theme') || 'light';
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
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
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
