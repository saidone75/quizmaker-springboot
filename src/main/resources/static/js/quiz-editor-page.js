(function () {
    const dataEl = document.getElementById('quiz-editor-data');

    window.CSRF_TOKEN = document.querySelector('meta[name="quizmaker-csrf-token"]')?.content || '';
    window.CSRF_HEADER = document.querySelector('meta[name="quizmaker-csrf-header"]')?.content || 'X-CSRF-TOKEN';

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
