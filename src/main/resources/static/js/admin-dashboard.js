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
    const isAdmin = document.getElementById('dashboard-page-data')?.dataset.isAdmin === 'true';
    let quizIdToShare = null;

    const deleteModal = document.getElementById('delete-modal');
    const deleteModalSubText = document.getElementById('modal-sub-text');
    const deleteModalConfirmBtn = document.getElementById('modal-confirm-btn');

    const shareModal = document.getElementById('share-modal');
    const shareModalSubText = document.getElementById('share-modal-sub-text');
    const shareModalConfirmBtn = document.getElementById('share-modal-confirm-btn');
    const shareSelectAll = document.getElementById('share-select-all');

    const closeDeleteModal = () => {
        if (deleteModal) {
            deleteModal.style.display = 'none';
        }
        if (deleteModalConfirmBtn) {
            deleteModalConfirmBtn.onclick = null;
        }
    };

    const closeShareModal = () => {
        if (shareModal) {
            shareModal.style.display = 'none';
        }
        quizIdToShare = null;
        shareSelectAll.checked = false;
        document.querySelectorAll('.share-teacher-checkbox').forEach((checkbox) => {
            checkbox.checked = false;
        });
    };

    const syncShareSelectAll = () => {
        const checkboxes = Array.from(document.querySelectorAll('.share-teacher-checkbox'));
        const allChecked = checkboxes.length > 0 && checkboxes.every((cb) => cb.checked);
        shareSelectAll.checked = allChecked;
    };

    const toggleShareSelectAll = (checked) => {
        document.querySelectorAll('.share-teacher-checkbox').forEach((checkbox) => {
            checkbox.checked = checked;
        });
    };

    const doShareQuiz = async () => {
        const selectedTeacherIds = Array.from(document.querySelectorAll('.share-teacher-checkbox:checked'))
            .map((checkbox) => checkbox.value);
        if (!selectedTeacherIds.length) {
            alert('Seleziona almeno un insegnante.');
            return;
        }

        const targetQuizId = quizIdToShare;
        closeShareModal();
        showLoading('Invio quiz in corso...');
        try {
            const res = await apiFetch('/api/quizzes/' + targetQuizId + '/share', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({teacherIds: selectedTeacherIds})
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            const copiedCount = await res.json();
            hideLoading();
            showToast('Quiz inviato a ' + copiedCount + ' insegnanti!');
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    };

    const doDelete = async (id) => {
        closeDeleteModal();
        showLoading('Eliminazione in corso...');
        try {
            const res = await apiFetch('/api/quizzes/' + id, {
                method: 'DELETE'
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            hideLoading();
            showToast('Quiz eliminato!');
            setTimeout(() => location.reload(), 1000);
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    };

    const togglePublication = async (checkbox) => {
        const id = checkbox.dataset.id;
        const published = checkbox.checked;
        checkbox.disabled = true;
        try {
            const res = await apiFetch('/api/quizzes/' + id + '/publication', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({published})
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            showToast(published ? 'Quiz pubblicato!' : 'Quiz nascosto agli studenti');
        } catch (e) {
            checkbox.checked = !published;
            alert('Errore: ' + e.message);
        } finally {
            checkbox.disabled = false;
        }
    };

    document.querySelectorAll('.js-open-delete-modal').forEach((button) => {
        button.addEventListener('click', () => {
            if (!deleteModal || !deleteModalSubText || !deleteModalConfirmBtn) {
                return;
            }
            const id = button.dataset.id;
            const title = button.dataset.title || 'questo quiz';
            deleteModalSubText.textContent = 'Stai per eliminare "' + title + '". Questa azione non può essere annullata.';
            deleteModalConfirmBtn.onclick = () => doDelete(id);
            deleteModal.style.display = 'flex';
        });
    });

    document.querySelectorAll('.js-open-share-modal').forEach((button) => {
        button.addEventListener('click', () => {
            if (!isAdmin || !shareModal || !shareModalSubText) {
                return;
            }
            quizIdToShare = button.dataset.id;
            const title = button.dataset.title || 'questo quiz';
            shareModalSubText.textContent = 'Seleziona i docenti destinatari per "' + title + '".';
            shareModal.style.display = 'flex';
        });
    });

    document.querySelectorAll('.js-toggle-publication').forEach((checkbox) => {
        checkbox.addEventListener('change', () => {
            togglePublication(checkbox);
        });
    });

    document.querySelectorAll('.share-teacher-checkbox').forEach((checkbox) => {
        checkbox.addEventListener('change', syncShareSelectAll);
    });

    shareSelectAll?.addEventListener('change', () => {
        toggleShareSelectAll(shareSelectAll.checked);
    });

    shareModalConfirmBtn?.addEventListener('click', doShareQuiz);
    document.getElementById('close-delete-modal-btn')?.addEventListener('click', closeDeleteModal);
    document.getElementById('close-share-modal-btn')?.addEventListener('click', closeShareModal);
})();
