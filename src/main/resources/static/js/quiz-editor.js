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

const EDITOR_LETTERS = ['A', 'B', 'C', 'D'];
const EDITOR_EMOJIS = ['❓','🦕','🔥','🌍','🎨','📚','🧪','🧠','🏆','🌟','🦴','🌿'];
let currentQuestions = [];
let quizId = null;

function initEditor(id, questionsJson) {
    quizId = id;
    if (questionsJson) {
        try {
            currentQuestions = Array.isArray(questionsJson)
                ? questionsJson
                : JSON.parse(questionsJson);
        } catch(e) {
            console.error('Errore parsing domande:', e);
            currentQuestions = [emptyQuestion()];
        }
    } else {
        currentQuestions = [emptyQuestion()];
    }
    renderQuestions();
}

function emptyQuestion() {
    return {
        text: '',
        emoji: EDITOR_EMOJIS[currentQuestions.length % EDITOR_EMOJIS.length] || '❓',
        options: ['', '', '', ''],
        answer: 0,
        feedback: ''
    };
}

function updateBadge() {
    const badge = document.getElementById('q-count-badge');
    if (badge) {
        badge.textContent = currentQuestions.length + ' domand' + (currentQuestions.length === 1 ? 'a' : 'e');
    }
}

function addQuestion() {
    currentQuestions.push({
        text: '',
        emoji: EDITOR_EMOJIS[currentQuestions.length % EDITOR_EMOJIS.length],
        options: ['', '', '', ''],
        answer: 0,
        feedback: ''
    });
    renderQuestions();
    setTimeout(() => {
        const cards = document.querySelectorAll('.q-card');
        if (cards.length > 0) {
            cards[cards.length - 1].scrollIntoView({ behavior: 'smooth', block: 'start' });
            toggleCard(currentQuestions.length - 1);
        }
    }, 50);
}

function deleteQuestion(idx) {
    if (currentQuestions.length <= 1) return;
    currentQuestions.splice(idx, 1);
    renderQuestions();
}

function toggleCard(idx) {
    const body = document.getElementById('qbody-' + idx);
    const toggle = document.getElementById('qtoggle-' + idx);
    if (!body || !toggle) return;
    const isOpen = body.classList.contains('open');
    document.querySelectorAll('.q-card-body').forEach(b => b.classList.remove('open'));
    document.querySelectorAll('.q-toggle').forEach(t => t.classList.remove('open'));
    if (!isOpen) {
        body.classList.add('open');
        toggle.classList.add('open');
    }
}

function setCorrect(qIdx, optIdx) {
    currentQuestions[qIdx].answer = optIdx;
    renderQuestions();
    setTimeout(() => {
        const body = document.getElementById('qbody-' + qIdx);
        const toggle = document.getElementById('qtoggle-' + qIdx);
        if (body) body.classList.add('open');
        if (toggle) toggle.classList.add('open');
    }, 10);
}

function syncField(qIdx, field, value) {
    currentQuestions[qIdx][field] = value;
    if (field === 'text') {
        const preview = document.getElementById('qpreview-' + qIdx);
        if (preview) {
            preview.textContent = value || 'Domanda senza testo...';
            preview.className = 'q-preview' + (value ? '' : ' empty');
        }
    }
}

function syncOption(qIdx, optIdx, value) {
    currentQuestions[qIdx].options[optIdx] = value;
}

function renderQuestions() {
    updateBadge();
    const list = document.getElementById('questions-list');
    if (!list) return;

    list.innerHTML = currentQuestions.map((q, i) => `
        <div class="q-card" id="qcard-${i}">
            <div class="q-card-header" onclick="toggleCard(${i})">
                <div class="q-num">${i + 1}</div>
                <div class="q-preview${q.text ? '' : ' empty'}" id="qpreview-${i}">${escHtml(q.text) || 'Domanda senza testo...'}</div>
                <div class="q-toggle" id="qtoggle-${i}">▼</div>
                <button class="q-delete" onclick="event.stopPropagation(); deleteQuestion(${i})" title="Elimina">✕</button>
            </div>
            <div class="q-card-body" id="qbody-${i}">
                <div class="q-row">
                    <div class="q-row-label">Testo della domanda</div>
                    <input type="text" class="input-field" placeholder="Scrivi la domanda..."
                           value="${escHtml(q.text)}"
                           oninput="syncField(${i}, 'text', this.value)">
                </div>
                <div class="q-row">
                    <div class="q-row-label">Emoji (opzionale)</div>
                    <input type="text" class="input-field" placeholder="❓"
                           value="${escHtml(q.emoji)}" maxlength="4" style="width:72px"
                           oninput="syncField(${i}, 'emoji', this.value)">
                </div>
                <div class="q-row">
                    <div class="q-row-label">Risposte — clicca la lettera per segnare quella corretta</div>
                    <div class="options-editor">
                        ${q.options.map((opt, j) => `
                            <div class="option-row">
                                <div class="option-letter${q.answer === j ? ' correct' : ''}"
                                     onclick="setCorrect(${i}, ${j})"
                                     title="Segna come corretta">${EDITOR_LETTERS[j]}</div>
                                <input type="text" class="input-field"
                                       placeholder="Risposta ${EDITOR_LETTERS[j]}..."
                                       value="${escHtml(opt)}"
                                       oninput="syncOption(${i}, ${j}, this.value)">
                            </div>
                        `).join('')}
                    </div>
                    <div class="correct-hint">Risposta corretta: <span>${EDITOR_LETTERS[q.answer]}</span> — clicca una lettera per cambiarla</div>
                </div>
                <div class="q-row">
                    <div class="q-row-label">Spiegazione (mostrata dopo la risposta)</div>
                    <input type="text" class="input-field"
                           placeholder="Es: Il fuoco serviva per scaldarsi e cucinare..."
                           value="${escHtml(q.feedback)}"
                           oninput="syncField(${i}, 'feedback', this.value)">
                </div>
            </div>
        </div>
    `).join('');
}

async function saveQuiz() {
    const title = document.getElementById('quiz-title-input').value.trim();
    const emoji = document.getElementById('quiz-emoji-input').value.trim() || '❓';
    const msgEl = document.getElementById('validation-msg');

    if (!title) { showValidation('Inserisci un nome per il quiz!'); return; }

    const valid = currentQuestions.filter(q =>
        q.text.trim() && q.options.filter(o => o.trim()).length >= 2
    );
    if (valid.length === 0) {
        showValidation('Aggiungi almeno una domanda con testo e almeno 2 risposte!');
        return;
    }

    const finalQuestions = valid.map(q => {
        const filledOpts = q.options.filter(o => o.trim());
        const answerText = q.options[q.answer];
        const newAnswerIdx = filledOpts.indexOf(answerText);
        return { ...q, options: filledOpts, answer: newAnswerIdx >= 0 ? newAnswerIdx : 0 };
    });

    msgEl.style.display = 'none';
    showLoading(quizId ? 'Salvataggio modifiche...' : 'Salvataggio in corso...');

    try {
        const url = quizId ? '/api/quizzes/' + quizId : '/api/quizzes';
        const method = quizId ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                [CSRF_HEADER]: CSRF_TOKEN
            },
            body: JSON.stringify({
                title,
                emoji,
                questions: finalQuestions
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Errore server');
        }

        hideLoading();
        showToast(quizId ? 'Quiz aggiornato!' : 'Quiz salvato!');
        setTimeout(() => { window.location.href = '/admin'; }, 1000);

    } catch(e) {
        hideLoading();
        showValidation('Errore: ' + e.message);
    }
}

function showValidation(text) {
    const msg = document.getElementById('validation-msg');
    if (msg) {
        msg.textContent = text;
        msg.style.display = 'block';
    }
}


function applyGeneratedQuiz(quiz) {
    document.getElementById('quiz-title-input').value = quiz.title || 'Quiz generato con AI';
    document.getElementById('quiz-emoji-input').value = quiz.emoji || '🤖';
    currentQuestions = (quiz.questions || []).map((q, idx) => ({
        text: q.text || '',
        emoji: q.emoji || EDITOR_EMOJIS[idx % EDITOR_EMOJIS.length],
        options: Array.isArray(q.options) ? q.options.slice(0, 4) : ['', '', '', ''],
        answer: Number.isInteger(q.answer) ? q.answer : 0,
        feedback: q.feedback || ''
    }));
    if (currentQuestions.length === 0) {
        currentQuestions = [emptyQuestion()];
    }
    renderQuestions();
}

async function generateQuizWithAi() {
    const msgEl = document.getElementById('ai-generation-msg');
    const topic = document.getElementById('ai-topic-input').value.trim();
    const fileInput = document.getElementById('ai-file-input');
    const numberOfQuestions = document.getElementById('ai-number-input').value;
    const difficulty = document.getElementById('ai-difficulty-input').value;
    const tone = document.getElementById('ai-tone-input').value;

    if (!topic && (!fileInput.files || fileInput.files.length === 0)) {
        msgEl.textContent = 'Inserisci un argomento o carica un file.';
        msgEl.style.display = 'block';
        return;
    }

    const formData = new FormData();
    formData.append('topic', topic || 'Usa solo il documento allegato come fonte principale.');
    formData.append('numberOfQuestions', numberOfQuestions);
    formData.append('difficulty', difficulty);
    formData.append('tone', tone);
    if (fileInput.files && fileInput.files.length > 0) {
        formData.append('file', fileInput.files[0]);
    }

    msgEl.style.display = 'none';
    showLoading('Generazione quiz con AI in corso...');

    try {
        const res = await fetch('/api/quizzes/generate', {
            method: 'POST',
            headers: {
                [CSRF_HEADER]: CSRF_TOKEN
            },
            body: formData
        });

        const payload = await res.json();
        if (!res.ok) {
            throw new Error(payload.message || 'Errore durante la generazione AI');
        }

        applyGeneratedQuiz(payload);
        hideLoading();
        showToast('Quiz generato! Controlla e salva.');
    } catch (e) {
        hideLoading();
        msgEl.textContent = 'Errore AI: ' + e.message;
        msgEl.style.display = 'block';
    }
}
