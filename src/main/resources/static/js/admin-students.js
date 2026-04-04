(function () {
    const csrfToken = document.querySelector('meta[name="quizmaker-csrf-token"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="quizmaker-csrf-header"]')?.content || '';

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
            const res = await fetch('/api/students', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
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
            const res = await fetch('/api/students/' + studentId, {
                method: 'DELETE',
                headers: {[csrfHeader]: csrfToken}
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
        showLoading('Rigenerazione password...');
        try {
            const res = await fetch('/api/students/' + studentId + '/regenerate-password', {
                method: 'POST',
                headers: {[csrfHeader]: csrfToken}
            });
            if (!res.ok) {
                const payload = await res.json();
                throw new Error(payload.message || 'Errore server');
            }
            const payload = await res.json();
            hideLoading();
            showToast('Nuova parola chiave: ' + payload.loginKeyword);
            setTimeout(() => location.reload(), 900);
        } catch (e) {
            hideLoading();
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
        showLoading('Rigenerazione password per tutti...');
        try {
            const res = await fetch('/api/students/regenerate-passwords', {
                method: 'POST',
                headers: {[csrfHeader]: csrfToken}
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            const regeneratedCount = await res.json();
            hideLoading();
            showToast('Password rigenerate per ' + regeneratedCount + ' studenti');
            setTimeout(() => location.reload(), 700);
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    });
})();
