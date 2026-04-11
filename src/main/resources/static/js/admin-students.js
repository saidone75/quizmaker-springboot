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
    const deleteModal = document.getElementById('delete-student-modal');
    const deleteModalSub = document.getElementById('delete-student-modal-sub');
    const deleteConfirmBtn = document.getElementById('delete-student-confirm-btn');

    const closeDeleteStudentModal = () => {
        if (deleteModal) {
            deleteModal.style.display = 'none';
        }
        if (deleteConfirmBtn) {
            deleteConfirmBtn.onclick = null;
        }
    };

    const printBtn = document.getElementById('print-students-btn');
    printBtn?.addEventListener('click', () => window.print());

    document.getElementById('create-student-form')?.addEventListener('submit', async (event) => {
        event.preventDefault();
        const input = document.getElementById('new-student-name');
        const fullName = (input?.value || '').trim();
        if (!fullName) {
            return;
        }

        showLoading('Creazione studente...');
        try {
            const res = await apiFetch('/api/students', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({fullName})
            });
            const payload = await res.json();
            if (!res.ok) {
                throw new Error(payload.message || 'Errore server');
            }
            hideLoading();
            showToast('Studente creato: ' + payload.loginKeyword);
            setTimeout(() => location.reload(), 700);
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    });

    const doDeleteStudent = async (studentId) => {
        closeDeleteStudentModal();
        showLoading('Eliminazione studente...');
        try {
            const res = await apiFetch('/api/students/' + studentId, {
                method: 'DELETE'
            });
            if (!res.ok) {
                const payload = await res.json();
                throw new Error(payload.message || 'Errore server');
            }
            hideLoading();
            showToast('Studente eliminato');
            setTimeout(() => location.reload(), 700);
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    };

    document.querySelectorAll('.js-delete-student').forEach((button) => {
        button.addEventListener('click', () => {
            const studentId = button.dataset.id;
            const studentName = button.dataset.name || 'questo studente';
            if (!studentId || !deleteModal || !deleteModalSub || !deleteConfirmBtn) {
                return;
            }
            deleteModalSub.textContent = 'Stai per eliminare "' + studentName + '". Questa azione non può essere annullata.';
            deleteConfirmBtn.onclick = () => doDeleteStudent(studentId);
            deleteModal.style.display = 'flex';
        });
    });

    document.getElementById('close-delete-student-modal-btn')?.addEventListener('click', closeDeleteStudentModal);

    const regenerateStudentPassword = async (studentId) => {
        try {
            const res = await apiFetch('/api/students/' + studentId + '/regenerate-password', {
                method: 'POST'
            });
            const payload = await res.json();
            if (!res.ok) {
                throw new Error(payload.message || 'Errore server');
            }
            showToast('Nuova parola chiave: ' + payload.loginKeyword);
            setTimeout(() => location.reload(), 900);
        } catch (e) {
            alert('Errore: ' + e.message);
        }
    };

    document.querySelectorAll('.js-regenerate-student-password').forEach((button) => {
        button.addEventListener('click', () => {
            const studentId = button.dataset.id;
            if (studentId) {
                regenerateStudentPassword(studentId);
            }
        });
    });

    document.getElementById('regenerate-all-passwords-btn')?.addEventListener('click', async () => {
        try {
            const res = await apiFetch('/api/students/regenerate-passwords', {
                method: 'POST'
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            const regeneratedCount = await res.json();
            showToast('Password rigenerate per ' + regeneratedCount + ' studenti');
            setTimeout(() => location.reload(), 700);
        } catch (e) {
            alert('Errore: ' + e.message);
        }
    });

    const copyKeyword = async (keyword) => {
        if (!keyword) {
            return;
        }

        const normalizedKeyword = keyword.trim();
        if (!normalizedKeyword) {
            return;
        }

        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(normalizedKeyword);
            return;
        }

        const textarea = document.createElement('textarea');
        textarea.value = normalizedKeyword;
        textarea.setAttribute('readonly', '');
        textarea.style.position = 'absolute';
        textarea.style.left = '-9999px';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    };

    document.querySelectorAll('.js-copy-keyword').forEach((keywordCell) => {
        keywordCell.addEventListener('click', async () => {
            const keyword = keywordCell.dataset.keyword || keywordCell.textContent;
            try {
                await copyKeyword(keyword || '');
                showToast('Parola chiave copiata negli appunti');
            } catch (e) {
                alert('Errore durante la copia della parola chiave');
            }
        });
    });
})();
