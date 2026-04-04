(function () {
    const csrfToken = document.querySelector('meta[name="quizmaker-csrf-token"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="quizmaker-csrf-header"]')?.content || '';

    const unlockSubmission = async (studentId, quizId) => {
        showLoading('Sblocco in corso...');
        try {
            const res = await fetch('/api/quizzes/' + quizId + '/unlock/' + studentId, {
                method: 'POST',
                headers: {[csrfHeader]: csrfToken}
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            hideLoading();
            showToast('Quiz sbloccato per lo studente');
            setTimeout(() => location.reload(), 700);
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    };

    const unlockAllForQuiz = async (quizId) => {
        showLoading('Sblocco quiz per tutti in corso...');
        try {
            const res = await fetch('/api/quizzes/' + quizId + '/unlock-all', {
                method: 'POST',
                headers: {[csrfHeader]: csrfToken}
            });
            if (!res.ok) {
                throw new Error('Errore server');
            }
            const unlockedCount = await res.json();
            hideLoading();
            showToast('Sbloccati ' + unlockedCount + ' studenti');
            setTimeout(() => location.reload(), 700);
        } catch (e) {
            hideLoading();
            alert('Errore: ' + e.message);
        }
    };

    document.querySelectorAll('.js-unlock-submission').forEach((button) => {
        button.addEventListener('click', () => {
            unlockSubmission(button.dataset.studentId, button.dataset.quizId);
        });
    });

    document.querySelectorAll('.js-unlock-all-quiz').forEach((button) => {
        button.addEventListener('click', () => {
            unlockAllForQuiz(button.dataset.quizId);
        });
    });
})();
