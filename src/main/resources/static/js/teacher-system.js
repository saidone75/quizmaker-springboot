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
