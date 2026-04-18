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

document.addEventListener('DOMContentLoaded', () => {
    const themeForm = document.getElementById('themePreferenceForm');
    const themeSelect = document.getElementById('themePreference');
    if (themeForm && themeSelect) {
        themeSelect.addEventListener('change', () => themeForm.submit());
    }

    const imageUploadForm = document.getElementById('imageUploadPreferenceForm');
    const imageUploadCheckbox = document.getElementById('imageUploadEnabled');
    if (imageUploadForm && imageUploadCheckbox) {
        imageUploadCheckbox.addEventListener('change', () => imageUploadForm.submit());
    }

    const imageSearchModeForm = document.getElementById('imageSearchModePreferenceForm');
    const imageSearchModeSelect = document.getElementById('imageSearchMode');
    if (imageSearchModeForm && imageSearchModeSelect) {
        imageSearchModeSelect.addEventListener('change', () => imageSearchModeForm.submit());
    }
});
