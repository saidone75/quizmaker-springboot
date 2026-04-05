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
    const unlockSubmission = async (studentId, quizId) => {
        showLoading('Sblocco in corso...');
        try {
            const res = await apiFetch('/api/quizzes/' + quizId + '/unlock/' + studentId, {
                method: 'POST'
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
            const res = await apiFetch('/api/quizzes/' + quizId + '/unlock-all', {
                method: 'POST'
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
