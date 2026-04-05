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

(function () {
    const deleteTeacherModal = document.getElementById('delete-teacher-modal');
    const deleteTeacherModalSub = document.getElementById('delete-teacher-modal-sub');
    const deleteTeacherConfirmBtn = document.getElementById('delete-teacher-confirm-btn');
    const deleteTeacherCancelBtn = document.getElementById('delete-teacher-cancel-btn');

    const closeDeleteTeacherModal = () => {
        if (deleteTeacherModal) {
            deleteTeacherModal.style.display = 'none';
        }
        if (deleteTeacherConfirmBtn) {
            deleteTeacherConfirmBtn.onclick = null;
        }
    };

    document.querySelectorAll('.teacher-ai-toggle-form input[type="checkbox"]').forEach((checkbox) => {
        checkbox.addEventListener('change', (event) => {
            const form = event.currentTarget.form;
            if (!form) {
                return;
            }
            const hiddenInput = form.querySelector('input[name="aiEnabled"]');
            if (hiddenInput) {
                hiddenInput.value = String(event.currentTarget.checked);
            }
            form.submit();
        });
    });

    document.querySelectorAll('.teacher-enabled-toggle-form input[type="checkbox"]').forEach((checkbox) => {
        checkbox.addEventListener('change', (event) => {
            const form = event.currentTarget.form;
            if (!form) {
                return;
            }
            const hiddenInput = form.querySelector('input[name="enabled"]');
            if (hiddenInput) {
                hiddenInput.value = String(event.currentTarget.checked);
            }
            form.submit();
        });
    });

    document.querySelectorAll('.delete-teacher-btn').forEach((button) => {
        button.addEventListener('click', () => {
            const form = button.closest('form');
            if (!form || !deleteTeacherModal || !deleteTeacherModalSub || !deleteTeacherConfirmBtn) {
                return;
            }
            const username = form.dataset.username || 'questo insegnante';
            deleteTeacherModalSub.textContent = `Stai per eliminare "${username}". Questa azione non può essere annullata.`;
            deleteTeacherConfirmBtn.onclick = () => form.submit();
            deleteTeacherModal.style.display = 'flex';
        });
    });

    if (deleteTeacherCancelBtn) {
        deleteTeacherCancelBtn.addEventListener('click', closeDeleteTeacherModal);
    }
})();
