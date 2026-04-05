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
    const dataEl = document.getElementById('student-page-data');
    const quizzesJson = dataEl?.dataset.quizzesJson || '[]';
    const lockedQuizIdsJson = dataEl?.dataset.lockedQuizIdsJson || '[]';

    const quizData = JSON.parse(quizzesJson);
    const lockedQuizIds = new Set(JSON.parse(lockedQuizIdsJson).map(String));
    const quizById = {};

    for (let i = 0; i < quizData.length; i++) {
        const quiz = quizData[i];
        quizById[String(quiz.id)] = quiz;
    }

    window.QUIZ_DATA_BY_ID = quizById;
    window.LOCKED_QUIZ_IDS = lockedQuizIds;

    document.getElementById('student-back-home-btn')?.addEventListener('click', () => {
        goTo('student');
    });
})();
