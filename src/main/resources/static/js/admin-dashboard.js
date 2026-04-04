(function () {
    const csrfToken = document.querySelector('meta[name="quizmaker-csrf-token"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="quizmaker-csrf-header"]')?.content || '';
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
            const res = await fetch('/api/quizzes/' + targetQuizId + '/share', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
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
            const res = await fetch('/api/quizzes/' + id, {
                method: 'DELETE',
                headers: {[csrfHeader]: csrfToken}
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
            const res = await fetch('/api/quizzes/' + id + '/publication', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
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
