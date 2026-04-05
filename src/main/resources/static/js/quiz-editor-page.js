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
    const dataEl = document.getElementById('quiz-editor-data');

    const rawQuizId = dataEl?.dataset.quizId;
    const quizId = rawQuizId && rawQuizId !== 'null' && rawQuizId !== '' ? rawQuizId : null;

    const rawQuestions = dataEl?.dataset.initialQuestionsJson;
    const initialQuestionsJson = rawQuestions && rawQuestions !== 'null' && rawQuestions !== '' ? rawQuestions : null;

    initEditor(quizId, initialQuestionsJson);

    document.getElementById('quiz-title-input')?.addEventListener('input', updateBadge);
    document.getElementById('add-question-btn')?.addEventListener('click', addQuestion);
    document.getElementById('save-btn')?.addEventListener('click', saveQuiz);
    document.getElementById('generate-quiz-ai-btn')?.addEventListener('click', generateQuizWithAi);
})();
