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

(function () {
    const profilePreference = document.querySelector('meta[name="quizmaker-theme-preference"]')?.content || '';
    const theme = profilePreference === 'light' || profilePreference === 'dark' || profilePreference === 'zenburn' || profilePreference === 'true-summer'
        ? profilePreference
        : 'light';

    const preloadBackground = theme === 'dark'
        ? '#2F3939'
        : (theme === 'zenburn' ? '#2D2D2D' : (theme === 'true-summer' ? '#EAF4FB' : '#F7F6F2'));

    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.style.backgroundColor = preloadBackground;
    document.documentElement.style.colorScheme = theme === 'light' ? 'light' : 'dark';

    const syncBodyTheme = function () {
        if (!document.body) {
            return;
        }
        document.body.setAttribute('data-theme', theme);
        document.body.style.backgroundColor = preloadBackground;
        document.body.style.colorScheme = theme === 'light' ? 'light' : 'dark';
    };

    syncBodyTheme();
    document.addEventListener('DOMContentLoaded', syncBodyTheme, {once: true});
})();
