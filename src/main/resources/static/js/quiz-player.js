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

const LETTERS = ['A', 'B', 'C', 'D'];
const QUIZ_PROGRESS_STORAGE_KEY = 'quizmaker.activeQuizProgress';
let playState = { quiz: null, current: 0, score: 0, wrong: 0, answered: false, answers: [] };
let studentAlertTimer = null;
let isQuizNavigationLocked = false;
let quizPopstateHandler = null;
let quizBeforeUnloadHandler = null;
let quizKeydownHandler = null;
let skipLogoutConfirmation = false;

function bindStudentLogoutConfirmation() {
    const logoutForm = document.querySelector('form[action="/student/logout"]');
    const exitModal = document.getElementById('student-exit-modal');
    const confirmBtn = document.getElementById('student-exit-confirm-btn');
    const cancelBtn = document.getElementById('student-exit-cancel-btn');
    if (!logoutForm || !exitModal || !confirmBtn || !cancelBtn) return;

    const closeExitModal = function () {
        exitModal.style.display = 'none';
        exitModal.setAttribute('aria-hidden', 'true');
    };

    const openExitModal = function () {
        exitModal.style.display = 'flex';
        exitModal.setAttribute('aria-hidden', 'false');
    };

    logoutForm.addEventListener('submit', function (event) {
        if (skipLogoutConfirmation || !isQuizNavigationLocked) {
            return;
        }
        event.preventDefault();
        openExitModal();
    });

    confirmBtn.addEventListener('click', function () {
        skipLogoutConfirmation = true;
        disableQuizNavigationLock();
        closeExitModal();
        logoutForm.submit();
    });

    cancelBtn.addEventListener('click', closeExitModal);
    exitModal.addEventListener('click', function (event) {
        if (event.target === exitModal) closeExitModal();
    });
}

function showStudentAlert(title, message) {
    const alertBox = document.getElementById('student-alert');
    if (!alertBox) {
        alert((title ? title + ': ' : '') + (message || ''));
        return;
    }

    const titleEl = alertBox.querySelector('.student-alert-title');
    const messageEl = alertBox.querySelector('.student-alert-text');
    titleEl.textContent = title || 'Quiz già completato';
    messageEl.textContent = message || 'Hai già finito questo quiz. Chiedi alla maestra di sbloccarlo.';

    alertBox.hidden = false;
    alertBox.classList.add('show');

    if (studentAlertTimer) {
        clearTimeout(studentAlertTimer);
    }
    studentAlertTimer = setTimeout(function() {
        alertBox.classList.remove('show');
        studentAlertTimer = setTimeout(function() {
            alertBox.hidden = true;
        }, 220);
    }, 3200);
}

function enableQuizNavigationLock() {
    if (isQuizNavigationLocked) return;
    isQuizNavigationLocked = true;

    history.pushState({ quizInProgress: true }, '', globalThis.location.href);

    quizPopstateHandler = function () {
        if (!isQuizNavigationLocked) return;
        history.pushState({ quizInProgress: true }, '', globalThis.location.href);
        showStudentAlert(
            'Quiz in corso',
            'Non puoi tornare indietro mentre stai facendo il quiz.'
        );
    };

    quizBeforeUnloadHandler = function (event) {
        if (!isQuizNavigationLocked) return;
        event.preventDefault();
        event.returnValue = '';
    };

    quizKeydownHandler = function (event) {
        if (!isQuizNavigationLocked) return;
        const isReloadKey = event.key === 'F5' || ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'r');
        if (!isReloadKey) return;
        event.preventDefault();
        showStudentAlert(
            'Quiz in corso',
            'Non puoi ricaricare la pagina mentre stai facendo il quiz.'
        );
    };

    globalThis.addEventListener('popstate', quizPopstateHandler);
    globalThis.addEventListener('beforeunload', quizBeforeUnloadHandler);
    globalThis.addEventListener('keydown', quizKeydownHandler);
}

function disableQuizNavigationLock() {
    if (!isQuizNavigationLocked) return;
    isQuizNavigationLocked = false;

    if (quizPopstateHandler) {
        globalThis.removeEventListener('popstate', quizPopstateHandler);
        quizPopstateHandler = null;
    }
    if (quizBeforeUnloadHandler) {
        globalThis.removeEventListener('beforeunload', quizBeforeUnloadHandler);
        quizBeforeUnloadHandler = null;
    }
    if (quizKeydownHandler) {
        globalThis.removeEventListener('keydown', quizKeydownHandler);
        quizKeydownHandler = null;
    }
}

function getActiveStudentContext() {
    const student = globalThis.ACTIVE_STUDENT_CONTEXT || {};
    return {
        id: String(student.id || '').trim(),
        name: String(student.name || '').trim()
    };
}

function isStoredProgressForActiveStudent(progress) {
    const activeStudent = getActiveStudentContext();
    const progressStudent = progress?.student || {};
    const storedId = String(progressStudent.id || '').trim();
    const storedName = String(progressStudent.name || '').trim();

    if (!activeStudent.id && !activeStudent.name) return true;
    if (storedId && activeStudent.id) return storedId === activeStudent.id;
    if (storedName && activeStudent.name) return storedName === activeStudent.name;
    return false;
}

function persistQuizProgress() {
    if (!playState?.quiz?.id) return;
    sessionStorage.setItem(QUIZ_PROGRESS_STORAGE_KEY, JSON.stringify({
        student: getActiveStudentContext(),
        quiz: {
            id: playState.quiz.id,
            title: playState.quiz.title,
            emoji: playState.quiz.emoji,
            questions: Array.isArray(playState.quiz.questions) ? playState.quiz.questions : []
        },
        quizId: String(playState.quiz.id),
        current: playState.current,
        score: playState.score,
        wrong: playState.wrong,
        answers: playState.answers
    }));
}

function clearQuizProgress() {
    sessionStorage.removeItem(QUIZ_PROGRESS_STORAGE_KEY);
}

function recomputeScoreFromAnswers() {
    if (!playState?.quiz?.questions) {
        playState.score = 0;
        playState.wrong = 0;
        return;
    }
    let score = 0;
    let wrong = 0;
    for (let i = 0; i < playState.quiz.questions.length; i++) {
        const answerIdx = playState.answers?.[i];
        if (typeof answerIdx !== 'number') continue;
        if (answerIdx === playState.quiz.questions[i].answer) score++;
        else wrong++;
    }
    playState.score = score;
    playState.wrong = wrong;
}

function resumeQuizIfNeeded() {
    const raw = sessionStorage.getItem(QUIZ_PROGRESS_STORAGE_KEY);
    if (!raw) return false;

    try {
        const progress = JSON.parse(raw);
        if (!isStoredProgressForActiveStudent(progress)) {
            clearQuizProgress();
            return false;
        }
        const quizSnapshot = progress.quiz;
        const quizId = String(progress.quizId || quizSnapshot?.id || '');
        const quizFromPage = globalThis.QUIZ_DATA_BY_ID?.[quizId];
        const quiz = (quizSnapshot && Array.isArray(quizSnapshot.questions) && quizSnapshot.questions.length > 0)
            ? quizSnapshot
            : quizFromPage;
        if (!quiz || !Array.isArray(quiz.questions) || quiz.questions.length === 0) {
            clearQuizProgress();
            return false;
        }

        const maxCurrent = Math.max(0, Math.min(Number(progress.current) || 0, quiz.questions.length - 1));
        playState = {
            quiz: { id: quiz.id, title: quiz.title, emoji: quiz.emoji, questions: quiz.questions },
            current: maxCurrent,
            score: 0,
            wrong: 0,
            answered: false,
            answers: Array.isArray(progress.answers) ? progress.answers : []
        };
        recomputeScoreFromAnswers();
        enableQuizNavigationLock();
        goTo('quiz');
        renderPlay();
        return true;
    } catch (_) {
        clearQuizProgress();
        return false;
    }
}
globalThis.resumeQuizIfNeeded = resumeQuizIfNeeded;

function startQuizFromCard(el) {
    const id = el.dataset.id;
    if (globalThis.LOCKED_QUIZ_IDS?.has(String(id)) || el.dataset.locked === 'true') {
        showStudentAlert();
        return;
    }

    const title = el.querySelector('.quiz-pick-name').textContent;
    const emoji = el.querySelector('.quiz-pick-icon').textContent;

    const questionsFromPage = globalThis.QUIZ_DATA_BY_ID?.[id]?.questions;
    if (Array.isArray(questionsFromPage)) {
        startQuiz({ id, title, emoji, questions: questionsFromPage });
        return;
    }

    fetch('/api/quizzes/' + id)
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Errore HTTP ' + response.status);
            }
            return response.json();
        })
        .then(function(quiz) {
            if (!quiz || !Array.isArray(quiz.questions)) {
                throw new Error('Quiz non valido');
            }
            startQuiz({
                id: String(quiz.id || id),
                title: quiz.title || title,
                emoji: quiz.emoji || emoji,
                questions: quiz.questions
            });
        })
        .catch(function() {
            showStudentAlert('Errore nel caricamento del quiz', 'Riprova tra poco o avvisa la maestra.');
        });
}

globalThis.startQuizFromCard = startQuizFromCard;


function markQuizCardAsLocked(quizId) {
    const quizCard = document.querySelector('.quiz-picker .quiz-pick-item[data-id="' + String(quizId) + '"]');
    if (!quizCard) return;

    quizCard.dataset.locked = 'true';
    quizCard.classList.add('is-locked');

}

function refreshLockedQuizCards() {
    const cards = document.querySelectorAll('.quiz-picker .quiz-pick-item');
    for (let i = 0; i < cards.length; i++) {
        const card = cards[i];
        if (globalThis.LOCKED_QUIZ_IDS?.has(String(card.dataset.id)) || card.dataset.locked === 'true') {
            markQuizCardAsLocked(card.dataset.id);
        }
    }
}
function bindQuizPickerCards() {
    refreshLockedQuizCards();
    const cards = document.querySelectorAll('.quiz-picker .quiz-pick-item');
    for (let i = 0; i < cards.length; i++) {
        const card = cards[i];
        card.onclick = function() {
            startQuizFromCard(card);
        };
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bindQuizPickerCards);
    document.addEventListener('DOMContentLoaded', bindStudentLogoutConfirmation);
} else {
    bindQuizPickerCards();
    bindStudentLogoutConfirmation();
}

function startQuiz(quiz) {
    playState = { quiz, current: 0, score: 0, wrong: 0, answered: false, answers: [] };
    enableQuizNavigationLock();
    persistQuizProgress();
    goTo('quiz');
    renderPlay();
}

function renderPlay() {
    recomputeScoreFromAnswers();
    const { quiz, current, score } = playState;
    const total = quiz.questions.length;
    const pct = Math.round((current / total) * 100);

    document.getElementById('play-title').textContent = quiz.emoji + ' ' + quiz.title;
    document.getElementById('play-score').textContent = '⭐ ' + score;
    document.getElementById('play-progress-text').textContent = `Domanda ${current + 1} di ${total}`;
    document.getElementById('play-pct').textContent = pct + '%';
    document.getElementById('play-progress-fill').style.width = pct + '%';

    if (current >= total) {
        showResult();
        return;
    }

    const q = quiz.questions[current];
    playState.answered = false;
    const imageUrl = resolveQuestionImageUrl(q);
    const mediaBlock = imageUrl ? `
            <div class="quiz-media-box">
                <img src="${escHtml(imageUrl)}" alt="Immagine domanda" class="quiz-media-img" loading="lazy" referrerpolicy="no-referrer">
            </div>
    ` : '';

    document.getElementById('play-area').innerHTML = `
        <div class="quiz-card">
            <span class="quiz-emoji">${q.emoji || '❓'}</span>
            <p class="quiz-question">${escHtml(q.text)}</p>
            ${mediaBlock}
            <div class="quiz-options">
                ${q.options.map((opt, i) => `
                    <button class="quiz-opt" data-answer-index="${i}">
                        <span class="ql">${LETTERS[i]}</span>
                        ${escHtml(opt)}
                    </button>
                `).join('')}
            </div>
            <div id="play-feedback"></div>
        </div>
        <button class="quiz-next" id="play-next" style="display:none">
            ${current < total - 1 ? 'Prossima domanda →' : 'Conferma risultato 🎉'}
        </button>
    `;

    document.querySelectorAll('.quiz-opt').forEach((button) => {
        button.addEventListener('click', () => {
            pickAnswer(Number(button.dataset.answerIndex));
        });
    });

    document.getElementById('play-next')?.addEventListener('click', nextQuestion);

    const existingAnswer = playState.answers[current];
    if (typeof existingAnswer === 'number') {
        pickAnswer(existingAnswer, true);
    }
}

function resolveQuestionImageUrl(question) {
    if (!question) return '';
    if (question.imageUrl?.trim()) return question.imageUrl.trim();
    if (question.imageId?.trim()) return '/api/quizzes/images/' + question.imageId.trim();
    return '';
}

function pickAnswer(idx, restoring) {
    if (playState.answered) return;
    const q = playState.quiz.questions[playState.current];
    if (typeof idx !== 'number' || idx < 0 || idx >= q.options.length) return;
    if (typeof playState.answers[playState.current] === 'number' && !restoring) {
        return;
    }
    playState.answered = true;
    const btns = document.querySelectorAll('.quiz-opt');
    btns.forEach(b => b.disabled = true);
    btns[q.answer].classList.add('correct');
    playState.answers[playState.current] = idx;

    const fb = document.getElementById('play-feedback');
    if (idx === q.answer) {
        recomputeScoreFromAnswers();
        document.getElementById('play-score').textContent = '⭐ ' + playState.score;
        fb.innerHTML = `<div class="quiz-feedback correct">✅ ${escHtml(q.feedback) || 'Esatto!'}</div>`;
    } else {
        recomputeScoreFromAnswers();
        btns[idx].classList.add('wrong');
        const correctText = q.options[q.answer];
        fb.innerHTML = `<div class="quiz-feedback wrong">❌ Risposta sbagliata! La risposta corretta era: <strong>${escHtml(correctText)}</strong>${q.feedback ? '. ' + escHtml(q.feedback) : '.'}</div>`;
    }
    persistQuizProgress();
    document.getElementById('play-next').style.display = 'block';
}

function nextQuestion() {
    playState.current++;
    persistQuizProgress();
    if (playState.current >= playState.quiz.questions.length) showResult();
    else renderPlay();
}

async function showResult() {
    const { score, wrong, quiz, answers } = playState;
    const total = quiz.questions.length;

    try {
        const res = await apiFetch('/api/quizzes/' + quiz.id + '/submit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers })
        });
        const payload = await res.json();
        if (!res.ok) {
            throw new Error(payload.message || 'Errore nel salvataggio');
        }
        if (globalThis.LOCKED_QUIZ_IDS) {
            globalThis.LOCKED_QUIZ_IDS.add(String(quiz.id));
        }
        markQuizCardAsLocked(quiz.id);
    } catch (e) {
        showStudentAlert('Errore nel salvataggio', e.message);
        clearQuizProgress();
        disableQuizNavigationLock();
        goTo('student');
        return;
    }

    clearQuizProgress();
    disableQuizNavigationLock();

    const pct = score / total;
    let emoji, title, stars;
    if (pct === 1)       { emoji = '🏆'; title = 'Perfetto! Sei un campione!'; stars = '⭐⭐⭐⭐⭐'; }
    else if (pct >= 0.8) { emoji = '🎉'; title = 'Fantastico! Ottimo lavoro!'; stars = '⭐⭐⭐⭐'; }
    else if (pct >= 0.6) { emoji = '😊'; title = 'Bravo! Hai fatto bene!'; stars = '⭐⭐⭐'; }
    else if (pct >= 0.4) { emoji = '👍'; title = 'Bene! Puoi migliorare!'; stars = '⭐⭐'; }
    else                 { emoji = '📚'; title = 'Studia ancora un po\'!'; stars = '⭐'; }

    document.getElementById('res-emoji').textContent = emoji;
    document.getElementById('res-title').textContent = title;
    document.getElementById('res-sub').textContent = 'Hai completato: ' + quiz.title;
    document.getElementById('res-score').innerHTML = score + '<span> / ' + total + '</span>';
    document.getElementById('res-stars').textContent = stars;
    document.getElementById('res-pills').innerHTML = `
        <span class="rpill rpill-ok">✅ ${score} corrette</span>
        <span class="rpill rpill-bad">❌ ${wrong} sbagliate</span>
    `;
    goTo('result');
}
