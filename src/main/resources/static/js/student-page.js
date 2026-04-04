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
    window.CSRF_TOKEN = document.querySelector('meta[name="quizmaker-csrf-token"]')?.content || '';
    window.CSRF_HEADER = document.querySelector('meta[name="quizmaker-csrf-header"]')?.content || '';

    document.getElementById('student-back-home-btn')?.addEventListener('click', () => {
        goTo('student');
    });
})();
