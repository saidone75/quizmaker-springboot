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

const LETTERS = ['A', 'B', 'C', 'D'];
let playState = { quiz: null, current: 0, score: 0, wrong: 0, answered: false, answers: [] };
let studentAlertTimer = null;

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

function startQuizFromCard(el) {
    const id = el.dataset.id;
    if ((window.LOCKED_QUIZ_IDS && window.LOCKED_QUIZ_IDS.has(String(id))) || el.dataset.locked === 'true') {
        showStudentAlert();
        return;
    }

    const title = el.querySelector('.quiz-pick-name').textContent;
    const emoji = el.querySelector('.quiz-pick-icon').textContent;

    const questionsFromPage =
        window.QUIZ_DATA_BY_ID &&
        window.QUIZ_DATA_BY_ID[id] &&
        window.QUIZ_DATA_BY_ID[id].questions;
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

window.startQuizFromCard = startQuizFromCard;


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
        if ((window.LOCKED_QUIZ_IDS && window.LOCKED_QUIZ_IDS.has(String(card.dataset.id))) || card.dataset.locked === 'true') {
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
} else {
    bindQuizPickerCards();
}

function startQuiz(quiz) {
    playState = { quiz, current: 0, score: 0, wrong: 0, answered: false, answers: [] };
    goTo('quiz');
    renderPlay();
}

function renderPlay() {
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

    document.getElementById('play-area').innerHTML = `
        <div class="quiz-card">
            <span class="quiz-emoji">${q.emoji || '❓'}</span>
            <p class="quiz-question">${escHtml(q.text)}</p>
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
}

function pickAnswer(idx) {
    if (playState.answered) return;
    playState.answered = true;

    const q = playState.quiz.questions[playState.current];
    const btns = document.querySelectorAll('.quiz-opt');
    btns.forEach(b => b.disabled = true);
    btns[q.answer].classList.add('correct');
    playState.answers[playState.current] = idx;

    const fb = document.getElementById('play-feedback');
    if (idx === q.answer) {
        playState.score++;
        document.getElementById('play-score').textContent = '⭐ ' + playState.score;
        fb.innerHTML = `<div class="quiz-feedback correct">✅ ${escHtml(q.feedback) || 'Esatto!'}</div>`;
    } else {
        playState.wrong++;
        btns[idx].classList.add('wrong');
        const correctText = q.options[q.answer];
        fb.innerHTML = `<div class="quiz-feedback wrong">❌ Risposta sbagliata! La risposta corretta era: <strong>${escHtml(correctText)}</strong>${q.feedback ? '. ' + escHtml(q.feedback) : '.'}</div>`;
    }
    document.getElementById('play-next').style.display = 'block';
}

function nextQuestion() {
    playState.current++;
    if (playState.current >= playState.quiz.questions.length) showResult();
    else renderPlay();
}

async function showResult() {
    const { score, wrong, quiz, answers } = playState;
    const total = quiz.questions.length;

    try {
        const headers = { 'Content-Type': 'application/json' };
        if (window.CSRF_HEADER && window.CSRF_TOKEN) {
            headers[window.CSRF_HEADER] = window.CSRF_TOKEN;
        }

        const res = await fetch('/api/quizzes/' + quiz.id + '/submit', {
            method: 'POST',
            headers,
            body: JSON.stringify({ answers })
        });
        const payload = await res.json();
        if (!res.ok) {
            throw new Error(payload.message || 'Errore nel salvataggio');
        }
        if (window.LOCKED_QUIZ_IDS) {
            window.LOCKED_QUIZ_IDS.add(String(quiz.id));
        }
        markQuizCardAsLocked(quiz.id);
    } catch (e) {
        showStudentAlert('Errore nel salvataggio', e.message);
        goTo('student');
        return;
    }

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
