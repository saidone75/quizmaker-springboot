const LETTERS = ['A', 'B', 'C', 'D'];
let playState = { quiz: null, current: 0, score: 0, wrong: 0, answered: false };

// Called from Thymeleaf student.html via onclick on card
function startQuizFromCard(el) {
    const id = el.dataset.id;
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

    const questionsJson = el.dataset.questions;
    if (questionsJson) {
        try {
            const questions = JSON.parse(questionsJson);
            if (Array.isArray(questions)) {
                startQuiz({ id, title, emoji, questions });
                return;
            }
        } catch (e) {
            // fallback to API request below
        }
    }

    fetch('/api/quizzes/' + id)
        .then(function(response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
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
            alert('Errore nel caricamento del quiz');
        });
}

window.startQuizFromCard = startQuizFromCard;

function bindQuizPickerCards() {
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
    playState = { quiz, current: 0, score: 0, wrong: 0, answered: false };
    goTo('quiz');
    renderPlay();
}

window.replayQuiz = function replayQuiz() {
    startQuiz(playState.quiz);
};

function renderPlay() {
    const { quiz, current, score } = playState;
    const total = quiz.questions.length;
    const pct = Math.round((current / total) * 100);

    document.getElementById('play-title').textContent = quiz.emoji + ' ' + quiz.title;
    document.getElementById('play-score').textContent = '⭐ ' + score;
    document.getElementById('play-progress-text').textContent = `Domanda ${current + 1} di ${total}`;
    document.getElementById('play-pct').textContent = pct + '%';
    document.getElementById('play-progress-fill').style.width = pct + '%';

    if (current >= total) { showResult(); return; }

    const q = quiz.questions[current];
    playState.answered = false;

    document.getElementById('play-area').innerHTML = `
        <div class="quiz-card">
            <span class="quiz-emoji">${q.emoji || '❓'}</span>
            <p class="quiz-question">${escHtml(q.text)}</p>
            <div class="quiz-options">
                ${q.options.map((opt, i) => `
                    <button class="quiz-opt" onclick="pickAnswer(${i})">
                        <span class="ql">${LETTERS[i]}</span>
                        ${escHtml(opt)}
                    </button>
                `).join('')}
            </div>
            <div id="play-feedback"></div>
        </div>
        <button class="quiz-next" id="play-next" style="display:none" onclick="nextQuestion()">
            ${current < total - 1 ? 'Prossima domanda →' : 'Vedi il risultato! 🎉'}
        </button>
    `;
}

window.pickAnswer = function pickAnswer(idx) {
    if (playState.answered) return;
    playState.answered = true;

    const q = playState.quiz.questions[playState.current];
    const btns = document.querySelectorAll('.quiz-opt');
    btns.forEach(b => b.disabled = true);
    btns[q.answer].classList.add('correct');

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
};

window.nextQuestion = function nextQuestion() {
    playState.current++;
    if (playState.current >= playState.quiz.questions.length) showResult();
    else renderPlay();
};

function showResult() {
    const { score, wrong, quiz } = playState;
    const total = quiz.questions.length;
    const pct = score / total;

    let emoji, title, stars;
    if (pct === 1)       { emoji = '🏆'; title = 'Perfetto! Sei un campione!';         stars = '⭐⭐⭐⭐⭐'; }
    else if (pct >= 0.8) { emoji = '🎉'; title = 'Fantastico! Ottimo lavoro!';         stars = '⭐⭐⭐⭐'; }
    else if (pct >= 0.6) { emoji = '😊'; title = 'Bravo! Hai fatto bene!';             stars = '⭐⭐⭐'; }
    else if (pct >= 0.4) { emoji = '👍'; title = 'Bene! Puoi migliorare!';             stars = '⭐⭐'; }
    else                 { emoji = '📚'; title = 'Studia ancora un po\'!';              stars = '⭐'; }

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
